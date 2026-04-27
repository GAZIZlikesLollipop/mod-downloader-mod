package org.gaziz.modrinthdirect.client.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
