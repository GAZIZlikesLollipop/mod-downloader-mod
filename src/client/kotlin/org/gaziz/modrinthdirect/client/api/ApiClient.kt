package org.gaziz.modrinthdirect.client.api

import com.luciad.imageio.webp.WebPReadParam
import com.mojang.blaze3d.platform.NativeImage
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import net.minecraft.client.Minecraft
import org.gaziz.modrinthdirect.client.ui.ModificationsScreen.formatTitle
import org.gaziz.modrinthdirect.client.api.models.DownloadState
import org.gaziz.modrinthdirect.client.api.models.ProjectResp
import org.gaziz.modrinthdirect.client.api.models.SearchHit
import org.gaziz.modrinthdirect.client.api.models.SearchResponse
import org.gaziz.modrinthdirect.client.api.models.VersionInfo
import java.io.File
import java.nio.file.Path
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import kotlin.io.path.createDirectories

object ApiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }

    private val client =  HttpClient(CIO){
        install(ContentNegotiation) {
            json(json)
        }
    }

    private val _searchedMods = MutableStateFlow<List<SearchHit>>(emptyList())
    val searchedMods: StateFlow<List<SearchHit>> = _searchedMods.asStateFlow()

    private val _downloadState = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadState: StateFlow<Map<String,DownloadState>> = _downloadState.asStateFlow()

    suspend fun removeDownloadState(slug: String){
        _downloadState.emit(downloadState.value.toMutableMap().apply { remove(slug) }.toMap())
    }

    suspend fun downloadPhoto(url: String): NativeImage {
        val rawBytes = client.get(url).bodyAsBytes()
        val contentType = client.get(url).headers["Content-Type"] ?: ""

        val bufferedImage = if (contentType.contains("webp")) {
            val reader: ImageReader = ImageIO.getImageReadersByMIMEType("image/webp").next()
            val param = WebPReadParam()
            param.isBypassFiltering = true
            reader.input = ImageIO.createImageInputStream(rawBytes.inputStream())
            reader.read(0, param)
        } else {
            ImageIO.read(rawBytes.inputStream()) ?: throw kotlinx.io.IOException("Cannot decode image")
        }

        val width = bufferedImage.width
        val height = bufferedImage.height
        val nativeImage = NativeImage(NativeImage.Format.RGBA, width, height, false)

        for (y in 0 until height) {
            for (x in 0 until width) {
                nativeImage.setPixel(x, y, bufferedImage.getRGB(x, y))
            }
        }

        return nativeImage
    }

    suspend fun search(
        query: String,
        limit: Int = 15
    ) {
        _searchedMods.emit(
            client
                .get(
                    "https://api.modrinth.com/v2/search"
                ) {
                    parameter("facets", "[[\"project_type:mod\"],[\"versions:1.21.11\"],[\"categories:fabric\"]]")
                    parameter("query", query)
                    parameter("limit", limit)
                }
                .body<SearchResponse>()
                .hits
        )
    }

    private suspend fun downloadMod(
        url: String,
        slug: String
    ) {
        val fileName = formatTitle(slug)
        try {
            val modsDir = "${Minecraft.getInstance().gameDirectory.path}/mods"
            val file = File("$modsDir/${fileName}.jar")
            Path.of(modsDir).createDirectories()
            file.writeBytes(client.get(url).bodyAsBytes())
        } catch (_: Exception) {
            _downloadState.emit(downloadState.value.toMutableMap().apply { set(slug,DownloadState.Error("download error")) })
        }
    }

    suspend fun startDownload(
        slug: String,
        isSetState: Boolean = true
    ) {
        if(isSetState) {
            _downloadState.emit(downloadState.value.toMutableMap().apply { set(slug,DownloadState.Loading) })
        }
        val versions = client.get(
            "https://api.modrinth.com/v2/project/$slug/version"
        ) {
            parameter("loaders","[\"fabric\"]")
            parameter("game_versions","[\"1.21.11\"]")
            parameter("include_changelog", false)
        }.body<List<VersionInfo>>()
        for(version in versions) {
            if(
                version.status == "listed" ||
                version.status == "archived"
            ) {
                for(depend in version.dependencies){
                    if(depend.dependencyType == "required") {
                        startDownload(client.get("https://api.modrinth.com/v2/project/${depend.projectId}").body<ProjectResp>().slug,false)
                    }
                }
                for(vFile in version.files) {
                    if(vFile.primary) {
                        downloadMod(vFile.url, slug)
                        if(isSetState) {
                            _downloadState.emit(downloadState.value.toMutableMap().apply { set(slug,DownloadState.OK) })
                        }
                        return
                    }
                }
            }
        }
        if(isSetState) {
            _downloadState.emit(downloadState.value.toMutableMap().apply { set(slug,DownloadState.Error("no installable files available")) })
        }
    }

}