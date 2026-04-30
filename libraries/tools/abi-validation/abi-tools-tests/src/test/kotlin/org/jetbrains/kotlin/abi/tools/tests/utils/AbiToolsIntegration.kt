/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalBuildToolsApi::class)

package org.jetbrains.kotlin.abi.tools.tests.utils

import java.io.File
import java.net.URLClassLoader
import org.jetbrains.kotlin.abi.tools.AbiTools
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.internal.KotlinBuildToolsInternalJdkUtils
import org.jetbrains.kotlin.buildtools.internal.getJdkClassesClassLoader

private const val ABI_TOOLS_EMBEDDABLE_PROPERTY = "abi.tools.embeddable.classpath"


val abiToolsEmbeddable = AbiTools.getInstance(createClassLoaderFromProperty())
val abiToolsOriginal = AbiTools.getInstance()


private fun createClassLoaderFromProperty(): ClassLoader {
    val classpath = System.getProperty(ABI_TOOLS_EMBEDDABLE_PROPERTY)
        ?: error("$ABI_TOOLS_EMBEDDABLE_PROPERTY property is not set")

    val urls =
        classpath.split(File.pathSeparator)
            .map { File(it).toURI().toURL() }

    println("Loading classes from property '$ABI_TOOLS_EMBEDDABLE_PROPERTY', classpath: $urls")
    return URLClassLoader(urls.toTypedArray(), SharedApiClassesClassLoader<AbiTools>())
}


@Suppress("TestFunctionName")
private inline fun <reified T> SharedApiClassesClassLoader(): ClassLoader = SharedApiClassesClassLoader(
    T::class.java.classLoader,
    T::class.java.`package`.name,
)

private class SharedApiClassesClassLoader(
    private val parent: ClassLoader,
    private val allowedPackage: String,
) : ClassLoader(@OptIn(KotlinBuildToolsInternalJdkUtils::class) getJdkClassesClassLoader()) {
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        return if (name.startsWith(allowedPackage)) {
            parent.loadClass(name)
        } else {
            super.loadClass(name, resolve)
        }
    }
}

