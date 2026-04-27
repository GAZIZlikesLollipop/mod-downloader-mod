package org.gaziz.modrinthdirect.client.api.models

import kotlinx.serialization.Serializable

@Serializable
data class ProjectResp(
    val slug: String
)

sealed interface DownloadState {
    object Loading: DownloadState
    data class Error(val message: String): DownloadState
    object OK: DownloadState
}
