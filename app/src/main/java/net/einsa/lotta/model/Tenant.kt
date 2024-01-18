package net.einsa.lotta.model

import kotlinx.serialization.Serializable
import net.einsa.lotta.GetTenantQuery

@Serializable()
data class Tenant(
    val id: ID,
    val title: String,
    val slug: String,
    val backgroundImageFileId: ID? = null,
    val logoImageFileId: ID? = null,
) {
    companion object {
        fun from(tenant: GetTenantQuery.Tenant): Tenant {
            return Tenant(
                id = tenant.id!!,
                title = tenant.title!!,
                slug = tenant.slug!!,
                backgroundImageFileId = tenant.configuration?.backgroundImageFile?.id,
                logoImageFileId = tenant.configuration?.logoImageFile?.id,
            )
        }
    }
}