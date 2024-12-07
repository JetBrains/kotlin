/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto

import java.io.File
import java.net.URLClassLoader

fun classLoaderForBackwardsCompatibleClasses(): ClassLoader {
    val uris = classpathForBackwardsCompatibleClasses().map { file -> file.toURI().toURL() }.toTypedArray()
    return URLClassLoader.newInstance(uris, null)
}

fun classpathForBackwardsCompatibleClasses(): List<File> {
    val compatibilityTestClasspath = System.getProperty("compatibilityTestClasspath")
        ?: error("Missing compatibilityTestClasspath system property")

    return compatibilityTestClasspath.split(";").map { path -> File(path) }
        .onEach { file -> if (!file.exists()) println("[WARNING] Missing $file") }
        .flatMap { file -> if (file.isDirectory) file.listFiles().orEmpty().toList() else listOf(file) }
}
