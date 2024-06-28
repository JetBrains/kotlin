/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.gradle.targets.js

data class Package(
    val name: String,
    val version: String,
    val displayName: String,
) {
    // Used in velocity template
    @Suppress("unused")
    fun camelize(): String =
        displayName
            .split("-")
            .mapIndexed { index, item ->
                if (index == 0) item else item.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            .joinToString("")
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