package org.gaziz.downloader.client

import com.luciad.imageio.webp.WebPReadParam
import com.mojang.blaze3d.platform.NativeImage
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.minecraft.client.Minecraft
import java.io.File
import javax.imageio.ImageIO
import javax.imageio.ImageReader

@Serializable
data class SearchHit(
    val project_id: String,

    val title: String,
    val description: String,
    val author: String,
    val downloads: Long,
    val follows: Long,
    val icon_url: String,
    val date_modified: String,
    val client_side: String,
    val server_side: String,
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
data class VersionInfo(
    val id: String,
    val files: List<VersionFile>,
    val status: String
)

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
        limit: Int = 10
    ): SearchResponse {
        return client
            .get(
                "https://api.modrinth.com/v2/search",
                {
                    parameter("facets", "[[\"project_type:mod\"],[\"versions:1.21.11\"],[\"categories:fabric\"]]")
                    parameter("query", query)
                    parameter("limit", limit)
                }
            )
            .body<SearchResponse>()
    }

    suspend fun downloadMod(
        projectId: String,
        fileName: String
    ): Exception? {
        val versions = client.get(
            "https://api.modrinth.com/v2/project/$projectId/version"
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
                for(vFile in version.files) {
                    if(vFile.primary) {
                        try {
                            val file = File("${Minecraft.getInstance().gameDirectory.path}/mods/${fileName}.jar")
                            file.writeBytes(client.get(vFile.url).bodyAsBytes())
                            return null
                        } catch (e: Exception) {
                            return e
                        }
                    }
                }
            }
        }
        return Exception("Mod files not found")
    }
}