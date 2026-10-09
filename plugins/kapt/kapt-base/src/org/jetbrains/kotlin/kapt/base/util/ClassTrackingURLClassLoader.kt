/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.base.util

import java.net.URL
import java.net.URLClassLoader
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * A [URLClassLoader] that remembers every class it defines.
 *
 * Memory leak detection needs the classes defined by the annotation processing classloader. Reading the private
 * `ClassLoader.classes` field only works on JDK 8: since JDK 12 all `ClassLoader` fields are filtered from reflection.
 *
 * [URLClassLoader] defines classes only from [findClass], so tracking its results covers all classes owned by this loader.
 */
class ClassTrackingURLClassLoader(urls: Array<URL>, parent: ClassLoader?) : URLClassLoader(urls, parent) {
    val definedClasses: Queue<Class<*>>
        field = ConcurrentLinkedQueue<Class<*>>()

    override fun findClass(name: String): Class<*> =
        super.findClass(name).also(definedClasses::add)
}
