/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.tests

import org.jetbrains.kotlin.tools.*
import org.junit.*
import org.junit.rules.TestName
import java.io.File

class CasesPublicAPITest {

    companion object {
        val baseClassPaths: List<File> =
            System.getProperty("testCasesClassesDirs")
                .let { requireNotNull(it) { "Specify testCasesClassesDirs with a system property"} }
                .split(File.pathSeparator)
                .map { File(it, "cases").canonicalFile }
        val baseOutputPath = File("src/test/kotlin/cases")
    }

    @[Rule JvmField]
    val testName = TestName()

    @Test fun companions() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun default() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun inline() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun interfaces() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun internal() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun java() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun localClasses() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun nestedClasses() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun private() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun protected() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun public() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun special() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun whenMappings() { snapshotAPIAndCompare(testName.methodName) }


    private fun snapshotAPIAndCompare(testClassRelativePath: String) {
        val testClassPaths = baseClassPaths.map { it.resolve(testClassRelativePath) }
        val testClasses = testClassPaths.flatMap { it.listFiles().orEmpty().asIterable() }
        check(testClasses.isNotEmpty()) { "No class files are found in paths: $testClassPaths" }

        val testClassStreams = testClasses.asSequence().filter { it.name.endsWith(".class") }.map { it.inputStream() }

        val api = getBinaryAPI(testClassStreams).filterOutNonPublic()

        val target = baseOutputPath.resolve(testClassRelativePath).resolve(testName.methodName + ".txt")

        api.dumpAndCompareWith(target)
    }
}
