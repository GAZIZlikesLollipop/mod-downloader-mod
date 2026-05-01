package org.gaziz.modrinthdirect.client.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchHit(
    val title: String,
    val slug: String,
    val description: String,
    val author: String? = null,
    val downloads: Long,
    val follows: Long? = null,
    val followers: Long? = null,
    val updated: String? = null,
    @SerialName("date_modified") val dateModified: String? = null,
    @SerialName("icon_url") val iconUrl: String,
    @SerialName("client_side") val clientSide: String,
    @SerialName("server_side") val serverSide: String,
)

@Serializable
data class SearchResponse(
    @SerialName("hits") val hits: List<SearchHit>
)