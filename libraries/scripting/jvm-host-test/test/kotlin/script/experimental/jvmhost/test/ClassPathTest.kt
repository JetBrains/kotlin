/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.test

import junit.framework.TestCase
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.net.URLClassLoader
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import kotlin.script.experimental.jvm.util.classPathFromTypicalResourceUrls
import kotlin.script.experimental.jvm.util.classpathFromClassloader
import kotlin.script.experimental.jvm.util.scriptCompilationClasspathFromContextOrNull

class ClassPathTest : TestCase() {

    lateinit var tempDir: File

    override fun setUp() {
        tempDir = createTempDir(ClassPathTest::class.simpleName!!)
        super.setUp()
    }

    override fun tearDown() {
        super.tearDown()
        tempDir.deleteRecursively()
    }

    @Test
    fun testExtractFromFat() {
        val collection = createTempFile("col", ".jar", directory = tempDir).apply { createCollectionJar(emulatedCollectionFiles, "BOOT-INF") }
        val cl = URLClassLoader(arrayOf(collection.toURI().toURL()), null)
        val cp = classpathFromClassloader(cl, true)
        Assert.assertTrue(cp != null && cp.isNotEmpty())

        testUnpackedCollection(cp!!, emulatedCollectionFiles)
    }

    @Test
    fun testDetectClasspathFromResources() {
        val root1 = createTempDir("root1", directory = tempDir)
        val jar = createTempFile("jar1", ".jar", directory = tempDir).apply { createJarWithManifest() }
        val cl = URLClassLoader(
            (emulatedClasspath.map { File(root1, it).apply { mkdirs() }.toURI().toURL() }
                    + jar.toURI().toURL()).toTypedArray(),
            null
        )
        val cp = cl.classPathFromTypicalResourceUrls().toList()

        Assert.assertTrue(cp.contains(jar.canonicalFile))
        for (el in emulatedClasspath) {
            Assert.assertTrue(cp.contains(File(root1, el).canonicalFile))
        }
    }

    @Test
    fun testFilterClasspath() {
        val tempDir = createTempDir().canonicalFile
        try {
            val files = listOf(
                File(tempDir, "projX/classes"),
                File(tempDir, "projX/test-classes"),
                File(tempDir, "projY/classes")
            )
            files.forEach { it.mkdirs() }

            val classloader = URLClassLoader(files.map { it.toURI().toURL() }.toTypedArray(), null)

            val classpath =
                scriptCompilationClasspathFromContextOrNull("projX", classLoader = classloader)!!.map { it.toRelativeString(tempDir) }

            Assert.assertEquals(files.dropLast(1).map { it.toRelativeString(tempDir) }, classpath)
        } finally {
            tempDir.deleteRecursively()
        }
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

fun File.createCollectionJar(fileNames: Array<String>, infDirName: String) {
    FileOutputStream(this).use { fileStream ->
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
        Assert.assertTrue(file.exists())
        Assert.assertEquals(it, file.readText())
    }

    val (classes, jars) = fileNames.partition { it.startsWith("classes") }
    val (cpClasses, cpJars) = classpath.partition { it.isDirectory && it.name == "classes" }
    Assert.assertTrue(cpClasses.size == 1)
    classes.checkFiles(cpClasses.first().parentFile)
    jars.checkFiles(cpJars.first().parentFile.parentFile)
}

fun File.createJarWithManifest() {
    FileOutputStream(this).use { fileStream ->
        val jarStream = JarOutputStream(fileStream, Manifest())
        jarStream.finish()
    }
}
