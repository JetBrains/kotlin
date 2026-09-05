/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api.jvm.java

import org.jetbrains.dokka.analysis.test.api.TestData
import org.jetbrains.dokka.analysis.test.api.TestDataFile
import org.jetbrains.dokka.analysis.test.api.util.filePathToPackageName

/**
 * A container for populating and holding Java source code test data.
 *
 * This container exists so that common creation, population and verification logic
 * can be reused, instead of having to implement [JavaFileCreator] multiple times.
 *
 * @param pathToJavaSources path to the `src` directory in which Java sources must reside.
 *                          Must be relative to the root of the test project. Example: `/src/main/java`
 * @see TestData
 */
class JavaTestData(
    private val pathToJavaSources: String
) : TestData, JavaFileCreator {

    private val files = mutableListOf<TestDataFile>()

    override fun javaFile(pathFromSrc: String, fillFile: JavaTestDataFile.() -> Unit) {
        val fileName = pathFromSrc.substringAfterLast("/")
        check(fileName.endsWith(".java")) { "Java files are expected to have .java extension" }

        val testDataFile = JavaTestDataFile(
            fullyQualifiedPackageName = filePathToPackageName(pathFromSrc),
            pathFromProjectRoot = "$pathToJavaSources/$pathFromSrc",
        )
        fillFile(testDataFile)
        testDataFile.checkFileNameIsPresent(fileName)
        files.add(testDataFile)
    }

    private fun JavaTestDataFile.checkFileNameIsPresent(fileName: String) {
        check(this.getContents().contains(fileName.removeSuffix(".java"))) {
            "Expected the .java file name to be the same as the top-level declaration name (class/interface)."
        }
    }

    override fun getFiles(): List<TestDataFile> {
        return files
    }

    override fun toString(): String {
        return "JavaTestData(pathToJavaSources='$pathToJavaSources', files=$files)"
    }
}

