/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin

import kotlinBuildProperties
import org.gradle.api.Project
import org.jetbrains.kotlin.konan.target.*

fun Project.kotlinInit(cacheRedirectorEnabled: Boolean) {
    extensions.extraProperties["defaultSnapshotVersion"] = kotlinBuildProperties.defaultSnapshotVersion
    extensions.extraProperties["kotlinVersion"] = findProperty("kotlinVersion")
}

fun String.splitCommaSeparatedOption(optionName: String) =
        split("\\s*,\\s*".toRegex()).map {
            if (it.isNotEmpty()) listOf(optionName, it) else listOf(null)
        }.flatten().filterNotNull()

data class Commit(val revision: String, val developer: String, val webUrlWithDescription: String)

val Project.platformManager
    get() = findProperty("platformManager") as PlatformManager

val validPropertiesNames = listOf(
        "konan.home",
        "org.jetbrains.kotlin.native.home",
        "kotlin.native.home"
)

val Project.kotlinNativeDist
    get() = rootProject.currentKotlinNativeDist

val Project.currentKotlinNativeDist
    get() = file(validPropertiesNames.firstOrNull { hasProperty(it) }?.let { findProperty(it) } ?: "dist")