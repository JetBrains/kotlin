/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.gradle.targets.js

import java.util.*

data class Package(
    val name: String,
    val version: String,
    val displayName: String,
) {
    // Note: this property is also used in the Velocity template
    val camelize: String =
        displayName
            .removePrefix("@")
            .split("-", "/")
            .joinToString("") { s -> s.replaceFirstChar { it.titlecase(Locale.ROOT) } }
            .replaceFirstChar { c -> c.lowercase(Locale.ROOT) }
}

sealed class PackageInformation {
    abstract val name: String
    abstract val versions: Set<String>
    abstract val displayName: String
}

data class RealPackageInformation(
    override val name: String,
    override val versions: Set<String>,
    override val displayName: String = name,
) : PackageInformation()

data class HardcodedPackageInformation(
    override val name: String,
    val version: String,
    override val displayName: String = name,
) : PackageInformation() {
    override val versions: Set<String> = setOf(version)
}
