/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.internal.build.instrumentation

import java.io.File
import java.net.URL
import java.util.ArrayList
import java.util.HashMap

internal object JavaRuntimeURLs {
    private val javaRuntimeFilesCache = HashMap<File, List<URL>>()

    fun hasJrt(javaHome: File): Boolean = javaHome.resolve("lib/jrt-fs.jar").isFile

    fun get(javaHome: File): List<URL> =
        javaRuntimeFilesCache.getOrPut(javaHome) {
            val urls = ArrayList<URL>()

            arrayOf("lib/rt.jar", "jre/lib/rt.jar", "bundle/Classes/classes.jar")
                .map { javaHome.resolve(it) }
                .filter { it.isFile }
                .mapTo(urls) { it.toURI().toURL() }

            if (hasJrt(javaHome)) {
                urls.add(URL("jrt", null, javaHome.path.replace(File.separatorChar, '/')))
            }

            if (urls.isEmpty()) throw RuntimeException("Could not find Java runtime for Java home: $javaHome")
            urls
        }

}