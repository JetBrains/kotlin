/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.base.util

import com.sun.tools.javac.util.Context
import java.net.URL
import java.util.Collections
import java.util.Enumeration
import java.util.concurrent.ConcurrentHashMap

/**
 * Returns a classloader that is able to load javac classes.
 *
 * The system classloader is not always enough on JDK 8. kapt CLI and Gradle workers can load `tools.jar` below it,
 * so javac's own [Context] gives the reliable classloader. [fallback] covers bootstrap-loaded javac.
 */
internal fun findClassLoaderWithJavac(fallback: ClassLoader = ClassLoader.getSystemClassLoader()): ClassLoader =
    Context::class.java.classLoader ?: fallback

/**
 * A parent for annotation processing classloaders that exposes only hosting JDK classes: platform classes and javac.
 *
 * [javacClassLoader] is a JDK 8 fallback for `tools.jar`; on JDK 9+ the platform classloader resolves javac
 * packages from `jdk.compiler`. The rest of the hosting classpath, including the Gradle daemon runtime, stays
 * hidden from annotation processors.
 */
class JdkOnlyParentClassLoader(private val javacClassLoader: ClassLoader) : ClassLoader(platformClassLoaderOrNull) {
    // registerAsParallelCapable() is caller-sensitive and cannot be called from a Kotlin companion object.
    // This loader never defines classes, so per-class-name locks are enough.
    private val classLoadingLocks = ConcurrentHashMap<String, Any>()

    override fun getClassLoadingLock(className: String): Any =
        classLoadingLocks.computeIfAbsent(className) { Any() }

    override fun loadClass(name: String, resolve: Boolean): Class<*> =
        try {
            super.loadClass(name, resolve)
        } catch (e: ClassNotFoundException) {
            if (name.isJavacClass()) {
                javacClassLoader.loadClass(name)
            } else {
                throw e
            }
        }

    // Keep javac resource visibility in sync with javac class visibility on JDK 8 tools.jar.
    override fun findResource(name: String): URL? =
        if (name.isJavacResource()) javacClassLoader.getResource(name) else null

    override fun findResources(name: String): Enumeration<URL> =
        if (name.isJavacResource()) javacClassLoader.getResources(name) else Collections.emptyEnumeration()

    private fun String.isJavacClass(): Boolean = JAVAC_PACKAGE_PREFIXES.any { startsWith(it) }

    private fun String.isJavacResource(): Boolean = JAVAC_RESOURCE_PREFIXES.any { startsWith(it) }

    companion object {
        private val JAVAC_PACKAGE_PREFIXES = listOf("com.sun.tools.", "com.sun.source.")

        private val JAVAC_RESOURCE_PREFIXES = JAVAC_PACKAGE_PREFIXES.map { it.replace('.', '/') }

        // null parent means the bootstrap classloader, which is correct for JDK 8:
        // there javax.annotation.processing and javax.lang.model come from rt.jar.
        private val platformClassLoaderOrNull: ClassLoader? =
            if (isJava9OrLater()) {
                ClassLoader::class.java.getMethod("getPlatformClassLoader").invoke(null) as ClassLoader
            } else {
                null
            }
    }
}
