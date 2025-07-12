package net.einsa.lotta.model

import kotlinx.serialization.Serializable

@Serializable()
data class TenantDescriptor(
    val id: Int,
    val title: String,
    val slug: String,
    val logoImageFileId: ID?,
    val backgroundImageFileId: ID?,
)

@Serializable()
data class ListTenantResult(
    val success: Boolean,
    val error: String? = null,
    val tenants: List<TenantDescriptor>? = null
)