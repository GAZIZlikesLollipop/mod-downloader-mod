package org.gaziz.downloader.client

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.gaziz.downloader.Moddownloadermod
import org.slf4j.LoggerFactory

@Serializable
data class SearchHit(
    val project_id: String,
    val versions: List<String>,

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

val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    isLenient = true
}

object DownloaderClient {
    private val logger = LoggerFactory.getLogger(Moddownloadermod.MOD_ID)
    val client =  HttpClient(CIO){
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {this@DownloaderClient.logger.info(message)}
            }
            level = LogLevel.ALL
        }
    }
    suspend fun search(query: String): SearchResponse {
        return client
            .get(
                "https://api.modrinth.com/v2/search?query=$query&limit=100",
                {
                    parameter("facets", "[[\"project_type:mod\"]]")
                }
            )
            .body<SearchResponse>()
    }
}