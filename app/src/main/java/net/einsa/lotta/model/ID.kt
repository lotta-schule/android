package net.einsa.lotta.model

typealias ID = String

fun ID.getUrl(tenant: Tenant, queryItems: Map<String, String> = emptyMap()): String {
    val searchParams = queryItems.map { "${it.key}=${it.value}" }.joinToString("&")
    return "https://${tenant.slug}.lotta.schule/storage/f/${this}?${searchParams}"
}