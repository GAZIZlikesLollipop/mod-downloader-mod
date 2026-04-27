package org.gaziz.modrinthdirect.client

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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.minecraft.client.Minecraft
import org.gaziz.modrinthdirect.client.ModificationsScreen.formatTitle
import java.io.File
import java.nio.file.Path
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import kotlin.io.path.createDirectories

@Serializable
data class SearchHit(
    val title: String,
    val slug: String,
    val description: String,
    val author: String,
    val downloads: Long,
    val follows: Long,
    @SerialName("icon_url") val iconUrl: String,
    @SerialName("date_modified") val dateModified: String,
    @SerialName("client_side") val clientSide: String,
    @SerialName("server_side") val serverSide: String,
)

@Serializable
data class SearchResponse(
    @SerialName("hits") val hits: List<SearchHit>
)

@Serializable
data class VersionFile(
    val url: String,
    val primary: Boolean
)

@Serializable
data class DependencyInfo(
    @SerialName("project_id") val projectId: String,
    @SerialName("dependency_type") val dependencyType: String
)

@Serializable
data class VersionInfo(
    val id: String,
    val files: List<VersionFile>,
    val status: String,
    val dependencies: List<DependencyInfo>
)

@Serializable
data class ProjectResp(
    val slug: String
)

sealed interface DownloadState {
    object Loading: DownloadState
    data class Error(val message: String): DownloadState
    object OK: DownloadState
}

val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    isLenient = true
}

object DownloaderClient {
    private val client =  HttpClient(CIO){
        install(ContentNegotiation) {
            json(json)
        }
    }

    private val _searchedMods = MutableStateFlow<List<SearchHit>>(emptyList())
    val searchedMods: StateFlow<List<SearchHit>> = _searchedMods.asStateFlow()

    private val _downloadState = MutableStateFlow<Map<String,DownloadState>>(emptyMap())
    val downloadState: StateFlow<Map<String,DownloadState>> = _downloadState.asStateFlow()

    suspend fun removeDownloadState(slug: String){
        _downloadState.emit(downloadState.value.toMutableMap().apply { remove(slug) }.toMap())
    }

    suspend fun downloadPhoto(url: String): NativeImage {
        val rawBytes = client.get(url).bodyAsBytes()
        val contentType = client.get(url).headers["Content-Type"] ?: ""

        val bufferedImage = if (contentType.contains("webp")) {
            val reader: ImageReader = ImageIO.getImageReadersByMIMEType("image/webp").next()
            val param = reader.defaultReadParam as WebPReadParam
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

    private suspend fun installDepend(slug: String) {
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
                        installDepend(client.get("https://api.modrinth.com/v2/project/${depend.projectId}").body<ProjectResp>().slug)
                    }
                }
                for(vFile in version.files) {
                    if(vFile.primary) {
                        downloadMod(vFile.url, slug)
                        return
                    }
                }
            }
        }
    }

    suspend fun startDownload(slug: String) {
        _downloadState.emit(downloadState.value.toMutableMap().apply { set(slug,DownloadState.Loading) })
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
                        installDepend(depend.projectId)
                    }
                }
                for(vFile in version.files) {
                    if(vFile.primary) {
                        downloadMod(vFile.url, slug)
                        _downloadState.emit(downloadState.value.toMutableMap().apply { set(slug,DownloadState.OK) })
                        return
                    }
                }
            }
        }
        _downloadState.emit(downloadState.value.toMutableMap().apply { set(slug,DownloadState.Error("no installable files available")) })
    }

}