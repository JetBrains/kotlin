/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.kgpnpmtooling.internal

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

/**
 * Representation of a `package.json` file.
 *
 * Only properties relevant for KGP's npm tooling dependencies are decoded,
 * to reduce the complexity and maintenance.
 */
@Serializable
internal data class PackageJson(
    val name: String,
    val version: String? = null,
    @EncodeDefault
    val private: Boolean = true,
    val dependencies: Map<String, String>,
)
