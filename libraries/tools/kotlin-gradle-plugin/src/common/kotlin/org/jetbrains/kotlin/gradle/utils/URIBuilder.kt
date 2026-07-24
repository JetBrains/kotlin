/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import java.net.URI

/**
 * Builder for URI with support for query parameter manipulation.
 * Note: When needed, this class can be modified with other mutating methods for scheme, authority, path, fragment, etc.
 */
internal class URIBuilder (
    private val scheme: String?,
    private val authority: String?,
    private val path: String?,
    private val queryParams: MutableMap<String, MutableList<String>>,
    private val fragment: String?,
) {
    /**
     * Adds a query parameter value without removing existing values for the same key.
     */
    fun addQueryParam(key: String, value: String): URIBuilder {
        queryParams.getOrPut(key) { mutableListOf() }.add(value)
        return this
    }

    /**
     * Builds the URI from the current components.
     */
    fun build(): URI {
        // Build raw (already URL-encoded) query string and assemble the URI from raw components
        // to avoid double-encoding that would occur with the multi-arg URI(...) constructor
        // (which encodes its inputs again).
        val rawQuery = if (queryParams.isEmpty()) {
            null
        } else {
            queryParams.entries.joinToString("&") { (key, values) ->
                values.joinToString("&") { value ->
                    "${encodeQueryComponent(key)}=${encodeQueryComponent(value)}"
                }
            }
        }

        // Reuse the original URI's raw scheme-specific-part formatting by reconstructing manually.
        val sb = StringBuilder()
        if (scheme != null) sb.append(scheme).append(':')
        if (authority != null) sb.append("//").append(authority)
        if (path != null) sb.append(path)
        if (rawQuery != null) sb.append('?').append(rawQuery)
        if (fragment != null) sb.append('#').append(fragment)
        return URI(sb.toString())
    }

    private fun encodeQueryComponent(component: String): String {
        return java.net.URLEncoder.encode(component, "UTF-8")
    }
}

/**
 * Creates a URIBuilder from the given URI, parsing all components including query parameters.
 * Query parameters with duplicate names are stored as multiple values for the same key.
 */
internal fun uriBuilder(base: URI): URIBuilder {
    val queryParams = mutableMapOf<String, MutableList<String>>()

    base.query?.split("&")?.forEach { param ->
        if (param.isNotEmpty()) {
            val parts = param.split("=", limit = 2)
            val key = java.net.URLDecoder.decode(parts[0], "UTF-8")
            val value = if (parts.size > 1) {
                java.net.URLDecoder.decode(parts[1], "UTF-8")
            } else {
                ""
            }
            queryParams.getOrPut(key) { mutableListOf() }.add(value)
        }
    }

    // Java's URI returns null for both "no authority" (file:/foo) and "empty authority" (file:///foo).
    // Distinguish by checking the raw scheme-specific-part: it starts with "//" iff the original had an authority component.
    val authority = base.authority
        ?: if (base.rawSchemeSpecificPart?.startsWith("//") == true) "" else null

    return URIBuilder(
        scheme = base.scheme,
        authority = authority,
        path = base.path,
        queryParams = queryParams,
        fragment = base.fragment
    )
}

/**
 * Creates a new URI combining [this] as base and [builder] as modifier.
 */
internal fun URI.withBuilder(builder: URIBuilder.() -> Unit): URI {
    return uriBuilder(this).apply(builder).build()
}
