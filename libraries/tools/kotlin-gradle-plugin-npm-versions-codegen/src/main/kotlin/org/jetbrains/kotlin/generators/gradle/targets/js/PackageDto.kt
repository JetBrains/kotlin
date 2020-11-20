/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.gradle.targets.js

data class Package(
    val name: String,
    val version: SemVer
) {
    // Used in velocity template
    @Suppress("unused")
    fun camelize(): String =
        name
            .split("-")
            .mapIndexed { index, item -> if (index == 0) item else item.capitalize() }
            .joinToString("")
}

data class PackageInformation(
    val name: String,
    val versions: Set<String>
)