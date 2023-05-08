/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.gradle.api.Project
import org.junit.Assume
import java.io.File

val androidSdk: String? = System.getProperty("android.sdk")
val isAndroidSdkAvailable: Boolean = androidSdk?.let { root -> File(root).exists() } ?: false

fun assumeAndroidSdkAvailable() {
    Assume.assumeTrue("Missing android sdk", isAndroidSdkAvailable)
}

fun setAndroidSdkDirProperty(project: Project) {
    val localPropertiesFile = File(project.rootDir.canonicalPath).resolve("local.properties")
    if (!localPropertiesFile.exists()) {
        localPropertiesFile.createNewFile()
    }
    localPropertiesFile.writeText("sdk.dir = $androidSdk")
}