/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import java.io.Serializable

class NpmOverride(
    val path: String
) : Serializable {
    var includedVersions = mutableListOf<String>()
    var excludedVersions = mutableListOf<String>()

    fun include(vararg include: String) {
        includedVersions.addAll(include)
    }

    fun exclude(vararg exclude: String) {
        excludedVersions.addAll(exclude)
    }
}

fun NpmOverride.toVersionString(): String {
    return buildNpmVersion(includedVersions, excludedVersions)
}