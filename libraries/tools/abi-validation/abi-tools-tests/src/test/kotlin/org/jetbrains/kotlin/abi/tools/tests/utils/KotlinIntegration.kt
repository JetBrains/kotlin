/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalBuildToolsApi::class)

package org.jetbrains.kotlin.abi.tools.tests.utils

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.SharedApiClassesClassLoader
import java.io.File
import java.net.URLClassLoader
import kotlin.io.path.toPath

private const val BUILD_TOOLS_IMPL_PROPERTY = "build.tools.impl.classpath"

val btaClassloader = createClassLoaderFromProperty()

fun loadToolchain(): KotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)

private fun createClassLoaderFromProperty(): ClassLoader {
    val classpath = System.getProperty(BUILD_TOOLS_IMPL_PROPERTY)
        ?: error("$BUILD_TOOLS_IMPL_PROPERTY property is not set")

    val urls =
        classpath.split(File.pathSeparator)
            .map { File(it).toURI().toURL() }

    println("Loading classes from property '$BUILD_TOOLS_IMPL_PROPERTY', classpath: $urls")
    return URLClassLoader(urls.toTypedArray(), SharedApiClassesClassLoader())
}

val currentKotlinStdlibLocation
    get() = btaClassloader.loadClass(KotlinVersion::class.qualifiedName).protectionDomain.codeSource.location.toURI().toPath()
