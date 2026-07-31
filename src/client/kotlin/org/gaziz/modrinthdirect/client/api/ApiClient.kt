package org.gaziz.modrinthdirect.client.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import net.minecraft.client.MinecraftClient
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.time.Duration
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import com.luciad.imageio.webp.WebPReadParam // Если используете WebP для чтения
import net.minecraft.client.texture.NativeImage
import org.gaziz.modrinthdirect.client.api.models.DownloadState
import org.gaziz.modrinthdirect.client.api.models.ProjectResp
import org.gaziz.modrinthdirect.client.api.models.SearchHit
import org.gaziz.modrinthdirect.client.api.models.SearchResponse
import org.gaziz.modrinthdirect.client.api.models.VersionInfo
import org.gaziz.modrinthdirect.client.data.InstalledMods
import org.gaziz.modrinthdirect.client.ui.state.StateHelper.formatTitle

object ApiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }

    private val client: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(15))
        .build()

    private val _searchedMods = MutableStateFlow<List<SearchHit>?>(null)
    val searchedMods: StateFlow<List<SearchHit>?> = _searchedMods.asStateFlow()

    private val _downloadState = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadState: StateFlow<Map<String, DownloadState>> = _downloadState.asStateFlow()

    private val logger = LoggerFactory.getLogger("modrinthdirect")

    private fun encodeParam(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8)

    private fun buildRequest(url: String): HttpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", "ModrinthDirect/1.0.0 (Minecraft Mod)")
            .GET()
            .build()

    private suspend inline fun <reified T> getJson(url: String): T = withContext(Dispatchers.IO) {
        val request = buildRequest(url)
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() !in 200..299) {
            throw IOException("HTTP Request failed with code: ${response.statusCode()} for $url")
        }

        json.decodeFromString<T>(response.body())
    }

    private suspend fun getBytes(url: String): HttpResponse<ByteArray> = withContext(Dispatchers.IO) {
        val request = buildRequest(url)
        val response = client.send(request, HttpResponse.BodyHandlers.ofByteArray())

        if (response.statusCode() !in 200..299) {
            throw IOException("HTTP Request failed with code: ${response.statusCode()} for $url")
        }

        response
    }

    suspend fun removeDownloadState(slug: String) {
        _downloadState.emit(downloadState.value.toMutableMap().apply { remove(slug) })
    }

    suspend fun downloadPhoto(url: String): NativeImage = withContext(Dispatchers.IO) {
        val response = getBytes(url)
        val rawBytes = response.body()
        val contentType = response.headers().firstValue("Content-Type").orElse("")

        val bufferedImage = if (contentType?.contains("webp") == true) {
            val reader: ImageReader = ImageIO.getImageReadersByMIMEType("image/webp").next()
            val param = WebPReadParam()
            param.isBypassFiltering = true
            reader.input = ImageIO.createImageInputStream(rawBytes.inputStream())
            reader.read(0, param)
        } else {
            ImageIO.read(rawBytes.inputStream()) ?: throw IOException("Cannot decode image")
        }

        val width = bufferedImage.width
        val height = bufferedImage.height
        val nativeImage = NativeImage(NativeImage.Format.RGBA, width, height, false)

        for (y in 0 until height) {
            for (x in 0 until width) {
                nativeImage.setColorArgb(x, y, bufferedImage.getRGB(x, y))
            }
        }

        nativeImage
    }

    suspend fun search(
        query: String,
        limit: Int = 12
    ) {
        val facets = encodeParam("[[\"project_type:mod\"],[\"versions:1.21.11\"],[\"categories:fabric\"]]")
        val encodedQuery = encodeParam(query)
        val url = "https://api.modrinth.com/v2/search?facets=$facets&query=$encodedQuery&limit=$limit"

        try {
            val response: SearchResponse = getJson(url)
            _searchedMods.emit(response.hits)
        } catch (e: Exception) {
            logger.error("Failed to search mods", e)
            _searchedMods.emit(emptyList())
        }
    }

    private suspend fun downloadMod(
        url: String,
        slug: String
    ) = withContext(Dispatchers.IO) {
        val fileName = formatTitle(slug)
        try {
            val modsDir = Path.of(MinecraftClient.getInstance().runDirectory.path, "mods")
            if (!modsDir.toFile().exists()) {
                modsDir.toFile().mkdirs()
            }
            val file = File(modsDir.toFile(), "$fileName.jar")

            val bytes = getBytes(url).body()
            file.writeBytes(bytes)

            InstalledMods.addMod(slug)
        } catch (e: Exception) {
            logger.error("Failed to download mod: $slug", e)
            _downloadState.emit(downloadState.value.toMutableMap().apply {
                set(slug, DownloadState.Error("download error"))
            })
        }
    }

    suspend fun startDownload(
        slug: String,
        isSetState: Boolean = true
    ) {
        if (isSetState) {
            _downloadState.emit(downloadState.value.toMutableMap().apply { set(slug, DownloadState.Loading) })
        }

        val loaders = encodeParam("[\"fabric\"]")
        val gameVersions = encodeParam("[\"1.21.11\"]")
        val url = "https://api.modrinth.com/v2/project/$slug/version?loaders=$loaders&game_versions=$gameVersions&include_changelog=false"

        try {
            val versions: List<VersionInfo> = getJson(url)

            for (version in versions) {
                if (version.status == "listed" || version.status == "archived") {

                    for (depend in version.dependencies) {
                        if (depend.dependencyType == "required") {
                            val depSlugUrl = "https://api.modrinth.com/v2/project/${depend.projectId}"
                            val depSlug = getJson<ProjectResp>(depSlugUrl).slug
                            startDownload(depSlug, false)
                        }
                    }

                    for (vFile in version.files) {
                        if (vFile.primary) {
                            downloadMod(vFile.url, slug)
                            if (isSetState) {
                                _downloadState.emit(downloadState.value.toMutableMap().apply { set(slug, DownloadState.OK) })
                            }
                            return
                        }
                    }
                }
            }

            if (isSetState) {
                _downloadState.emit(downloadState.value.toMutableMap().apply {
                    set(slug, DownloadState.Error("no installable files available"))
                })
            }
        } catch (e: Exception) {
            logger.error("Error during download process for $slug", e)
            if (isSetState) {
                _downloadState.emit(downloadState.value.toMutableMap().apply {
                    set(slug, DownloadState.Error(e.message ?: "download error"))
                })
            }
        }
    }

    suspend fun getInstalled(mods: List<String>) {
        try {
            val jsonIds = json.encodeToString(mods).replace(" ", "").replace("\n", "")
            val encodedIds = encodeParam(jsonIds)
            val url = "https://api.modrinth.com/v2/projects?ids=$encodedIds"

            val result: List<SearchHit> = getJson(url)
            _searchedMods.emit(result)
        } catch (e: Exception) {
            logger.error("Failed to fetch installed mods info", e)
            _searchedMods.emit(emptyList())
        }
    }
}