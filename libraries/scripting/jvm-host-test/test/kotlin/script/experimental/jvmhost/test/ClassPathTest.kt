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
import kotlin.script.experimental.jvm.util.classpathFromClassloader

class ClassPathTest : TestCase() {

    @Test
    fun testExtractFromFat() {
        val collection = createTempFile("col", ".jar").apply { createCollectionJar(emulatedCollectionFiles, "BOOT-INF") }
        val cl = URLClassLoader(arrayOf(collection.toURI().toURL()), null)
        val cp = classpathFromClassloader(cl, true)
        Assert.assertTrue(cp != null && cp.isNotEmpty())

        testUnpackedCollection(cp!!, emulatedCollectionFiles)
    }
}

private val emulatedCollectionFiles = arrayOf(
    "classes/a/b.class",
    "lib/c-d.jar"
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