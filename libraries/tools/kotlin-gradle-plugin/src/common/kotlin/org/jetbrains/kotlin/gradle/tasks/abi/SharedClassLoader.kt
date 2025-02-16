/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.abi

import org.jetbrains.kotlin.abi.tools.api.AbiToolsFactory
import org.jetbrains.kotlin.buildtools.internal.KotlinBuildToolsInternalJdkUtils
import org.jetbrains.kotlin.buildtools.internal.getJdkClassesClassLoader
import org.jetbrains.kotlin.gradle.internal.ParentClassLoaderProvider

/**
 * A class loader to share some classes from the [sharedClassLoader] with the package name that starts with [sharedPackage].
 *
 * If a class belongs to [sharedPackage] it is taken from the [sharedClassLoader]. Otherwise, it is taken from the JDK class loader.
 */
internal class SharedClassLoader(
    private val sharedClassLoader: ClassLoader,
    private val sharedPackage: String,
) : ClassLoader(@OptIn(KotlinBuildToolsInternalJdkUtils::class) (getJdkClassesClassLoader())) {
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        return if (name.startsWith(sharedPackage)) {
            sharedClassLoader.loadClass(name)
        } else {
            super.loadClass(name, resolve)
        }
    }
}

/**
 * A provider for the shared class loader that shares classes from the `org.jetbrains.kotlin.abi.tools.api` package and its subpackages.
 *
 * It serves as the parent class loader, ensuring that all its heirs use the same loaded classes for the `org.jetbrains.kotlin.abi.tools.api` package.
 */
internal object SharedClassLoaderProvider : ParentClassLoaderProvider {
    override fun getClassLoader() = createSharedClassLoader()

    override fun hashCode() = SharedClassLoaderProvider::class.hashCode()

    override fun equals(other: Any?) = other is SharedClassLoaderProvider

    private fun createSharedClassLoader(): ClassLoader {
        return SharedClassLoader(
            AbiToolsFactory::class.java.classLoader,
            AbiToolsFactory::class.java.`package`.name,
        )
    }
}