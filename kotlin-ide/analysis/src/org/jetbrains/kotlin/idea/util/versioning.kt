/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util

import org.jetbrains.kotlin.idea.artifacts.KotlinArtifacts
import java.io.File

fun isEap(version: String): Boolean {
    return version.contains("rc") || version.contains("eap") || version.contains("-M") || version.contains("RC")
}

fun isDev(version: String): Boolean {
    return version.contains("dev")
}

fun isSnapshot(version: String): Boolean {
    return version.contains("SNAPSHOT", ignoreCase = true)
}

val buildNumber: String by lazy {
    val versionFile = File(KotlinArtifacts.instance.kotlincDirectory, "build.txt")
    if (versionFile.exists()) versionFile.readText().trim() else "unknown"
}
