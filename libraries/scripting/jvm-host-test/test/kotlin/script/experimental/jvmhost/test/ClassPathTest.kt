/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalPathApi::class)

package kotlin.script.experimental.jvmhost.test

import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import kotlin.io.deleteRecursively
import kotlin.io.path.*
import kotlin.script.experimental.jvm.util.classPathFromTypicalResourceUrls
import kotlin.script.experimental.jvm.util.classpathFromClass
import kotlin.script.experimental.jvm.util.classpathFromClassloader
import kotlin.script.experimental.jvm.util.scriptCompilationClasspathFromContextOrNull
import kotlin.test.Test
import kotlin.test.BeforeTest
import kotlin.test.AfterTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClassPathTest {

    lateinit var tempDir: Path

    @BeforeTest
    fun setUp() {
        tempDir = createTempDirectory(ClassPathTest::class.simpleName!!)
    }

    @AfterTest
    fun tearDown() {
        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun testExtractFromFat() {
        val collection =
            createTempFile(directory = tempDir, "col", ".jar").apply { createCollectionJar(emulatedCollectionFiles, "BOOT-INF") }
        val cl = URLClassLoader(arrayOf(collection.toUri().toURL()), null)
        val cp = classpathFromClassloader(cl, true)
        assertTrue(cp != null && cp.isNotEmpty())

        testUnpackedCollection(cp, emulatedCollectionFiles)
    }

    @Test
    fun testDetectClasspathFromResources() {
        val root1 = createTempDirectory(directory = tempDir, "root1")
        val jar = createTempFile(directory = tempDir, "jar1", ".jar").apply { createJarWithManifest() }
        val cl = URLClassLoader(
            (emulatedClasspath.map { (root1 / it).apply { createDirectories() }.toUri().toURL() }
                    + jar.toUri().toURL()).toTypedArray(),
            null
        )
        val cp = cl.classPathFromTypicalResourceUrls().toList().map { it.canonicalFile }

        assertTrue(cp.contains(jar.toFile().canonicalFile))
        for (el in emulatedClasspath) {
            assertTrue(cp.contains((root1 / el).toFile().canonicalFile))
        }
    }

    @Test
    fun testFilterClasspath() {
        val tempDir = createTempDirectory().toRealPath()
        try {
            val files = listOf(
                (tempDir / "projX/classes"),
                (tempDir / "projX/test-classes"),
                (tempDir / "projY/classes")
            )
            files.forEach { it.createDirectories() }

            val classloader = URLClassLoader(files.map { it.toUri().toURL() }.toTypedArray(), null)

            val classpath =
                scriptCompilationClasspathFromContextOrNull("projX", classLoader = classloader)!!.map { it.toPath().relativeTo(tempDir) }

            assertEquals(files.dropLast(1).map { it.relativeTo(tempDir) }, classpath)
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun testClasspathFromClass() {
        val cpFromThis = classpathFromClass(this::class)
        val expectedSuffix = File("classes/kotlin/test").path
        assertTrue(
            cpFromThis!!.first().absoluteFile.path.endsWith(expectedSuffix),
            "Path should end with $expectedSuffix, got: $cpFromThis"
        )
    }
}

private val emulatedCollectionFiles = arrayOf(
    "classes/a/b.class",
    "lib/c-d.jar"
)

private val emulatedClasspath = arrayOf(
    "module1/classes/kotlin/main/",
    "module2/classes/java/test/"
)

fun Path.createCollectionJar(fileNames: Array<String>, infDirName: String) {
    this.outputStream().use { fileStream ->
        val jarStream = JarOutputStream(fileStream)
        jarStream.putNextEntry(JarEntry("$infDirName/classes/"))
        jarStream.putNextEntry(JarEntry("$infDirName/lib/"))
        for (name in fileNames) {
            jarStream.putNextEntry(JarEntry("$infDirName/$name"))
            jarStream.write(name.toByteArray())
        }
        jarStream.finish()
    }
}

fun testUnpackedCollection(classpath: List<File>, fileNames: Array<String>) {

    fun List<String>.checkFiles(root: File) = forEach {
        val file = File(root, it)
        assertTrue(file.exists())
        assertEquals(it, file.readText())
    }

    val (classes, jars) = fileNames.partition { it.startsWith("classes") }
    val (cpClasses, cpJars) = classpath.partition { it.isDirectory && it.name == "classes" }
    assertTrue(cpClasses.size == 1)
    classes.checkFiles(cpClasses.first().parentFile)
    jars.checkFiles(cpJars.first().parentFile.parentFile)
}

fun Path.createJarWithManifest() {
    this.outputStream().use { fileStream ->
        val jarStream = JarOutputStream(fileStream, Manifest())
        jarStream.finish()
    }
}
