package net.einsa.lotta.model

typealias ID = String

fun ID.getDownloadUrl(tenant: Tenant, format: String = "original"): String {
    return "https://${tenant.slug}.lotta.schule/data/storage/f/$this/$format"
}

fun ID.getDownloadUrl(tenant: TenantDescriptor, format: String = "original"): String {
    return "https://${tenant.slug}.lotta.schule/data/storage/f/$this/$format"
}
