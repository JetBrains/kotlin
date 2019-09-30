/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm.test

import junit.framework.TestCase
import org.junit.Test
import java.io.File
import java.util.jar.JarFile
import java.util.jar.JarInputStream
import kotlin.script.experimental.jvm.util.*

class UtilsTest : TestCase() {

    @Test
    fun testPatternConversionWildcards() {
        assertPattern("a${pathSeparatorPattern}b\\.$pathElementPattern", "a/b.*")
        assertPattern("a$pathSeparatorPattern$pathElementPattern\\.txt", "a/*.txt")
        assertPattern("a$pathSeparatorPattern.*/b", "a/**/b")
        assertPattern("a${pathSeparatorPattern}b.\\.txt", "a/b?.txt")
        assertPattern("$pathElementPattern/b\\.txt", "*/b.txt")
        assertPattern(".*${pathSeparatorPattern}b\\.txt", "**/b.txt")
    }

    @Test
    fun testPatternConversionEscaping() {
        assertPattern("aa\\+\\(\\)\\[\\]\\^\\\$\\{\\}\\|", "aa+()[]^\${}|")
        assertPattern("\\+\\(\\)\\[\\]\\^\\\$\\{\\}\\|bb", "+()[]^\${}|bb")
    }

    @Test
    fun testSelectFilesInDir() {

        val rootDir = File(".")

        fun assertProjectFilesBy(pattern: String, vararg paths: String) {
            val res = ArrayList<Pair<String, String>>()

            forAllMatchingFilesInDirectory(rootDir, pattern) { path, stream ->
                res.add(path to stream.reader().readText())
            }
            assertEquals(paths.toSet(), res.mapTo(HashSet()) { it.first })

            res.forEach { (path, bytes) ->
                val data = File(path).readText()
                assertEquals("Mismatching data for $path", data, bytes)
            }
        }

        assertProjectFilesBy("*.kt") // none
        assertProjectFilesBy("**/sss/*.kt") // none
        assertProjectFilesBy(
            "src/kotlin/script/experimental/jvm/util/jvmClassLoaderUtil.kt",
            "src/kotlin/script/experimental/jvm/util/jvmClassLoaderUtil.kt"
        )
        assertProjectFilesBy(
            "src/kotlin/script/experimental/jvm/util/jvm?lassLoaderUtil.kt",
            "src/kotlin/script/experimental/jvm/util/jvmClassLoaderUtil.kt"
        )
        assertProjectFilesBy(
            "src/kotlin/script/experimental/jvm/util/jvm*LoaderUtil.kt",
            "src/kotlin/script/experimental/jvm/util/jvmClassLoaderUtil.kt"
        )
        assertProjectFilesBy("**/jvmClassLoaderUtil.kt", "src/kotlin/script/experimental/jvm/util/jvmClassLoaderUtil.kt")
        assertProjectFilesBy("**/script/**/jvmClassLoaderUtil.kt", "src/kotlin/script/experimental/jvm/util/jvmClassLoaderUtil.kt")
        assertProjectFilesBy("src/**/jvmClassLoaderUtil.kt", "src/kotlin/script/experimental/jvm/util/jvmClassLoaderUtil.kt")
        assertProjectFilesBy("test/**/?????Test.*", "test/kotlin/script/experimental/jvm/test/utilsTest.kt")

        val allSrcKtFiles = HashSet<String>()
        forAllMatchingFilesInDirectory(rootDir, "src/**/*.kt") { path, _ ->
            allSrcKtFiles.add(path)
        }
        val allExpectedSrcKtFiles =
            rootDir.walkTopDown().filter {
                it.relativeToOrSelf(rootDir).path.startsWith("src") && it.extension == "kt"
            }.mapTo(HashSet()) {
                it.relativeToOrSelf(rootDir).path
            }
        assertEquals(allExpectedSrcKtFiles, allSrcKtFiles)
    }

    @Test
    fun testSelectFilesInJar() {

        fun JarFile.filesBy(pattern: String): Map<String, String> {
            val res = HashMap<String, String>()
            forAllMatchingFilesInJarFile(this, namePatternToRegex(pattern)) { path, stream ->
                res[path] = stream.reader().readText().trim()
            }
            return res
        }

        fun JarInputStream.filesBy(pattern: String): Map<String, String> {
            val res = HashMap<String, String>()
            forAllMatchingFilesInJarStream(this, namePatternToRegex(pattern)) { path, stream ->
                res[path] = stream.reader().readText().trim()
            }
            return res
        }

        fun assertFiles(actual: Map<String, String>, vararg expected: Pair<String, String>) {
            val expectedAsMap = expected.toMap()
            assertEquals(expectedAsMap, actual)
        }

        fun assertMatchingFilesInJarTwoWay(jar: File, pattern: String, vararg expected: Pair<String, String>) {
            assertFiles( JarFile(jar).filesBy(pattern), *expected)
            assertFiles( JarInputStream(jar.inputStream()).use { it.filesBy(pattern) }, *expected)
        }

        val jar = File("testData/testJar.jar")
        assertTrue(jar.exists())

        assertMatchingFilesInJarTwoWay(jar, "META-INF/*.kotlin_module", "META-INF/abc.kotlin_module" to "module")
        assertMatchingFilesInJarTwoWay(jar, "META-INF/*.kotlin") // none
        assertMatchingFilesInJarTwoWay(jar, "**/*.class", "a/b/c/d1.class" to "d1", "a/b/c/d1\$s1.class" to "d1s1")
        assertMatchingFilesInJarTwoWay(jar, "**/*\$*.class", "a/b/c/d1\$s1.class" to "d1s1")
    }

    private fun assertPattern(expected: String, pattern: String) {
        assertEquals(expected, namePatternToRegex(pattern).pattern)
    }

}
