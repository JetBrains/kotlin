/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.kapt.classloaders

import com.google.gson.Gson
import org.junit.Test
import java.io.File
import java.net.URLDecoder
import java.nio.charset.Charset
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class ClassLoadersCacheTest {

    private val rootClassLoader = this.javaClass.classLoader.rootOrSelf()

    private val someClass = Test::class.java
    private val someJar = findJarByClass(someClass)!!

    private val otherClass = Gson::class.java
    private val otherJar = findJarByClass(otherClass)!!

    @Test
    fun testNewClassLoader() {
        val cache = ClassLoadersCache(10, rootClassLoader)
        val cl = cache.getForClassPath(listOf(someJar))
        val loaded = cl.loadClass(someClass.name)
        assertNotSame(someClass, loaded, "Class should be from different ClassLoader")
    }

    @Test
    fun testCacheClassLoader() {
        val cache = ClassLoadersCache(10, rootClassLoader)
        val cp = listOf(someJar)

        val cl1 = cache.getForClassPath(cp)
        val loaded1 = cl1.loadClass(someClass.name)

        val cl2 = cache.getForClassPath(cp)
        val loaded2 = cl2.loadClass(someClass.name)

        assertSame(loaded2, loaded1, "Should return the same ClassLoader for same class path")
    }

    @Test
    fun testDifferentClassPath() {
        val cache = ClassLoadersCache(10, rootClassLoader)

        val cl1 = cache.getForClassPath(listOf(someJar))
        val loaded1 = cl1.loadClass(someClass.name)

        val cl2 = cache.getForClassPath(listOf(someJar, otherJar))
        val loaded2 = cl2.loadClass(someClass.name)

        assertNotSame(loaded2, loaded1, "Should create different ClassLoaders for different class paths")
    }

    @Test
    fun testSplitClassPath() {
        val cache = ClassLoadersCache(10, rootClassLoader)
        val topCp = listOf(someJar)
        val bottomCp1 = listOf(otherJar)
        val bottomCp2 = listOf(otherJar, findJarByClass(JvmField::class.java)!!)

        val cl1 = cache.getForSplitPaths(bottomCp1, topCp)
        val cl2 = cache.getForSplitPaths(bottomCp2, topCp)

        assertSame(
            cl1.loadClass(someClass.name),
            cl2.loadClass(someClass.name),
            "Top classpath should be cached separately. ClassLoader shouldn't change if top classpath stays the same"
        )
        assertNotSame(
            cl1.loadClass(otherClass.name),
            cl2.loadClass(otherClass.name),
            "Bottom ClassLoader should be recreated as class path changed"
        )
    }

    private fun findJarByClass(klass: Class<*>): File? {
        val classFileName = klass.name.substringAfterLast(".") + ".class"
        val resource = klass.getResource(classFileName) ?: return null
        val uri = resource.toString()
        if (!uri.startsWith("jar:file:")) return null

        val fileName = URLDecoder.decode(
            uri.removePrefix("jar:file:").substringBefore("!"),
            Charset.defaultCharset().name()
        )
        return File(fileName)
    }
}
