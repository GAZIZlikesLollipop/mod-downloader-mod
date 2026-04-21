package org.gaziz.downloader.client

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
    val clint_side: String,
    val server_side: String,
)

data class SearchResponse(
    val hits: List<SearchHit>,
)

object DownloaderClient {
    fun fuck() {
    }
}