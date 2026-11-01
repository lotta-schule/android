package net.einsa.lotta.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable()
data class TenantDescriptor(
    val id: Int,
    val title: String,
    val slug: String,
    @SerialName("logoImageFile")
    val logoImageFileId: ID?,
    @SerialName("backgroundImageFile")
    val backgroundImageFileId: ID?,
)

@Serializable()
data class ListTenantResult(
    val success: Boolean,
    val error: String? = null,
    val tenants: List<TenantDescriptor>? = null
)