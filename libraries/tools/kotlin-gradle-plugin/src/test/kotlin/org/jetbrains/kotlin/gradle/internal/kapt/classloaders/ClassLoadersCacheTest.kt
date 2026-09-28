/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.kapt.classloaders

import com.google.gson.Gson
import org.jetbrains.kotlin.gradle.internal.KaptExecutionToken
import kotlin.test.Test
import java.io.File
import java.net.URLDecoder
import java.net.URLClassLoader
import java.nio.charset.Charset
import java.nio.file.Files
import java.util.concurrent.Executors
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.test.*

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
    fun testCacheableProcessorClasspathIsReusedAndExecutionLocalProcessorClasspathIsRecreated() {
        val cache = ClassLoadersCache(10, rootClassLoader)
        val cacheableProcessorClasspath = listOf(someJar)
        val executionLocalProcessorClasspath1 = listOf(otherJar)
        val executionLocalProcessorClasspath2 = listOf(otherJar, findJarByClass(JvmField::class.java)!!)

        val executionToken = KaptExecutionToken()
        try {
            val cl1 = cache.getForProcessorClasspath(
                executionToken = executionToken,
                executionLocalProcessorClasspath = executionLocalProcessorClasspath1,
                cacheableProcessorClasspath = cacheableProcessorClasspath,
            )
            val cl2 = cache.getForProcessorClasspath(
                executionToken = executionToken,
                executionLocalProcessorClasspath = executionLocalProcessorClasspath2,
                cacheableProcessorClasspath = cacheableProcessorClasspath,
            )

            assertSame(
                cl1.loadClass(someClass.name),
                cl2.loadClass(someClass.name),
                "Cacheable processor classpath should be reused across execution-local classpath changes"
            )
            assertNotSame(
                cl1.loadClass(otherClass.name),
                cl2.loadClass(otherClass.name),
                "Execution-local processor classpath should get a fresh ClassLoader when it changes"
            )
        } finally {
            cache.releaseExecutionLocalLoaders(executionToken)
        }
    }

    @Test
    fun testExecutionLocalLoadersCanBeCreatedOnDifferentThreadAndReleasedByToken() {
        val cache = ClassLoadersCache(10, rootClassLoader)
        val temporaryDirectory = Files.createTempDirectory("kapt-classloaders-cache-test")
        try {
            val jar = temporaryDirectory.resolve("resources.jar").toFile()
            createResourceJar(jar)

            val executionToken = KaptExecutionToken()
            val classLoader = try {
                val executor = Executors.newSingleThreadExecutor()
                val classLoader = try {
                    executor.submit<ClassLoader> {
                        cache.getForProcessorClasspath(
                            executionToken = executionToken,
                            executionLocalProcessorClasspath = listOf(jar),
                            cacheableProcessorClasspath = emptyList(),
                        )
                    }.get() as URLClassLoader
                } finally {
                    executor.shutdownNow()
                }
                assertNotNull(classLoader.getResource("available-before-close.txt"))
                classLoader
            } finally {
                cache.releaseExecutionLocalLoaders(executionToken)
            }
            assertNull(classLoader.getResource("available-after-close.txt"))
        } finally {
            temporaryDirectory.toFile().deleteRecursively()
        }
    }

    @Test
    fun testReleasedExecutionTokenCannotBeReused() {
        val cache = ClassLoadersCache(10, rootClassLoader)
        val executionToken = KaptExecutionToken()
        cache.releaseExecutionLocalLoaders(executionToken)

        assertFailsWith<IllegalStateException> {
            cache.getForProcessorClasspath(
                executionToken = executionToken,
                executionLocalProcessorClasspath = listOf(otherJar),
                cacheableProcessorClasspath = emptyList(),
            )
        }
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

    private fun createResourceJar(file: File) {
        JarOutputStream(file.outputStream()).use { jar ->
            listOf("available-before-close.txt", "available-after-close.txt").forEach { resourceName ->
                jar.putNextEntry(JarEntry(resourceName))
                jar.write(resourceName.toByteArray())
                jar.closeEntry()
            }
        }
    }
}
