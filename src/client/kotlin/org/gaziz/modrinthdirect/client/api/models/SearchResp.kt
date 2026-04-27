package org.gaziz.modrinthdirect.client.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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