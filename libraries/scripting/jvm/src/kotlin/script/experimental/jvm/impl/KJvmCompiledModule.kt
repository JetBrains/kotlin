/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm.impl

import java.io.File
import java.io.InputStream
import java.net.URL
import java.net.URLClassLoader
import java.util.*

interface KJvmCompiledModule {
    fun createClassLoader(baseClassLoader: ClassLoader?): ClassLoader
}

class KJvmCompiledModuleFromClassPath(val classpath: Collection<File>) : KJvmCompiledModule {

    override fun createClassLoader(baseClassLoader: ClassLoader?): ClassLoader =
        URLClassLoader(classpath.map { it.toURI().toURL() }.toTypedArray(), baseClassLoader)
}

class KJvmCompiledModuleFromClassLoader(val moduleClassLoader: ClassLoader) : KJvmCompiledModule {

    override fun createClassLoader(baseClassLoader: ClassLoader?): ClassLoader =
        if (baseClassLoader == null) moduleClassLoader
        else DualClassLoader(moduleClassLoader, baseClassLoader)
}

private class DualClassLoader(fallbackLoader: ClassLoader, parentLoader: ClassLoader?) :
    ClassLoader(singleClassLoader(fallbackLoader, parentLoader) ?: parentLoader) {

    private class Wrapper(parent: ClassLoader) : ClassLoader(parent) {
        fun openFindResources(name: String): Enumeration<URL> = super.findResources(name)
        fun openFindResource(name: String): URL? = super.findResource(name)
    }

    companion object {
        private fun singleClassLoader(fallbackLoader: ClassLoader, parentLoader: ClassLoader?): ClassLoader? {
            tailrec fun ClassLoader.isAncestorOf(other: ClassLoader?): Boolean = when {
                other == null -> false
                this === other -> true
                else -> isAncestorOf(other.parent)
            }

            return when {
                parentLoader == null -> fallbackLoader
                parentLoader.isAncestorOf(fallbackLoader) -> fallbackLoader
                fallbackLoader.isAncestorOf(parentLoader) -> parentLoader
                else -> null
            }
        }
    }

    private val fallbackClassLoader: Wrapper? =
        if (/* optimization */ parentLoader == null || singleClassLoader(fallbackLoader, parentLoader) != null) null
        else Wrapper(fallbackLoader)

    override fun findClass(name: String): Class<*> = try {
        super.findClass(name)
    } catch (e: ClassNotFoundException) {
        fallbackClassLoader?.loadClass(name) ?: throw e
    }

    override fun getResourceAsStream(name: String): InputStream? =
        super.getResourceAsStream(name) ?: fallbackClassLoader?.getResourceAsStream(name)

    override fun findResources(name: String): Enumeration<URL> =
        if (fallbackClassLoader == null) super.findResources(name)
        else Collections.enumeration(super.findResources(name).toList() + fallbackClassLoader.openFindResources(name).asSequence())

    override fun findResource(name: String): URL? =
        super.findResource(name) ?: fallbackClassLoader?.openFindResource(name)
}
