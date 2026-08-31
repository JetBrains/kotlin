/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.base.util

/**
 * A parent for the annotation processing classloader that exposes only JDK classes of the hosting process:
 * platform classes (through the platform classloader on JDK 9+, or the bootstrap classloader on JDK 8)
 * and the javac implementation.
 *
 * [javacClassLoader] is consulted for javac packages only: on JDK 9+ the `jdk.compiler` module is defined
 * by the application classloader, and on JDK 8 javac comes from `tools.jar`, so these classes cannot be
 * loaded through the platform classloader.
 *
 * Everything else visible in the hosting process (e.g. the Gradle daemon runtime classpath, KT-88583)
 * is hidden from annotation processors, which otherwise shadows their own dependencies
 * because of the parent-first delegation.
 */
class JdkOnlyParentClassLoader(private val javacClassLoader: ClassLoader) : ClassLoader(platformClassLoaderOrNull) {
    override fun loadClass(name: String, resolve: Boolean): Class<*> =
        try {
            super.loadClass(name, resolve)
        } catch (e: ClassNotFoundException) {
            if (JAVAC_PACKAGE_PREFIXES.any { name.startsWith(it) }) {
                javacClassLoader.loadClass(name)
            } else {
                throw e
            }
        }

    companion object {
        private val JAVAC_PACKAGE_PREFIXES = listOf("com.sun.tools.", "com.sun.source.")

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
