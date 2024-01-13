package net.einsa.lotta.model

import net.einsa.lotta.GetTenantQuery

data class Tenant(
    val id: ID,
    val title: String,
    val slug: String,
    val backgroundImageFileId: ID? = null,
    val logoImageFileId: ID? = null,
) {
    companion object {
        fun from(tenantData: GetTenantQuery.Data): Tenant {
            return Tenant(
                id = tenantData.tenant!!.id!!,
                title = tenantData.tenant.title!!,
                slug = tenantData.tenant.slug!!,
                backgroundImageFileId = tenantData.tenant.configuration?.backgroundImageFile?.id,
                logoImageFileId = tenantData.tenant.configuration?.logoImageFile?.id,
            )
        }
    }
}