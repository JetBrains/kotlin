/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.legacy

import org.jetbrains.kotlin.abi.tools.api.*
import org.jetbrains.kotlin.abi.tools.v2.ToolsV2
import org.junit.*
import org.junit.rules.TestName
import java.io.File
import java.nio.file.Path
import kotlin.io.path.walk
import kotlin.test.fail

class CasesPublicAPITest {

    companion object {
        val baseClassPaths: List<File> =
            System.getProperty("testCasesClassesDirs")
                .let { requireNotNull(it) { "Specify testCasesClassesDirs with a system property"} }
                .split(File.pathSeparator)
                .map { File(it, "cases").canonicalFile }
        val baseOutputPath = File("src/test/kotlin/cases")
    }

    @Rule
    @JvmField
    val testName = TestName()

    @Test fun annotations() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun companions() { snapshotAPIAndCompare(testName.methodName, "cases.companions.PrivateApi") }

    @Test fun default() { snapshotAPIAndCompare(testName.methodName, "cases.default.PrivateApi") }

    @Test fun inline() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun interfaces() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun internal() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun java() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun localClasses() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun marker() { snapshotAPIAndCompare(testName.methodName, "cases.marker.HiddenField", "cases.marker.HiddenProperty", "cases.marker.HiddenMethod") }

    @Test fun nestedClasses() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun packageAnnotations() { snapshotAPIAndCompare(testName.methodName, "cases.packageAnnotations.PrivateApi") }

    @Test fun private() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun protected() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun public() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun special() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun suspend() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun whenMappings() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun enums() { snapshotAPIAndCompare(testName.methodName) }

    private fun snapshotAPIAndCompare(testClassRelativePath: String, vararg excludeAnnotatedWith: String) {
        val testClassPaths = baseClassPaths.map { it.resolve(testClassRelativePath) }
        val testClasses = testClassPaths.flatMap { it.walk() }.filter { it.name.endsWith(".class") }
        check(testClasses.isNotEmpty()) { "No class files are found in paths: $testClassPaths" }

        val target = baseOutputPath.resolve(testClassRelativePath).resolve(testName.methodName + ".txt")

        val filters = AbiFilters(emptySet(), emptySet(), emptySet(), excludeAnnotatedWith.toSet())

        if (!target.exists()) {
            target.bufferedWriter().use { writer ->
                ToolsV2.printJvmDump(writer, testClasses, filters)
            }
            fail("Expected data file did not exist. Generating: $target")
        } else {
            val stringBuffer = StringBuffer()
            ToolsV2.printJvmDump(stringBuffer, testClasses, filters)
            assertEqualsToFile(target, stringBuffer.toString())
        }
    }
}
