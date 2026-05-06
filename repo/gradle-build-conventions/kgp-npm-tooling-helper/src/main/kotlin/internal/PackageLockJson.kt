/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.kgpnpmtooling.internal

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Representation of a `package-lock.json` file.
 *
 * Only properties relevant for KGP's npm tooling dependencies are decoded,
 * to reduce the complexity and maintenance.
 */
@Serializable
internal data class PackageLockJson(
    val name: String? = null,
    val packages: Map<String, PackageEntry> = emptyMap(),
) {

    @Serializable
    data class PackageEntry(
        val version: String? = null,
        val dependencies: Map<String, String> = emptyMap(),
    )

    companion object {
        private val json: Json = Json {
            ignoreUnknownKeys = true
        }

        internal fun decodeFromString(content: String): PackageLockJson {
            return json.decodeFromString(PackageLockJson.serializer(), content)
        }
    }
}
