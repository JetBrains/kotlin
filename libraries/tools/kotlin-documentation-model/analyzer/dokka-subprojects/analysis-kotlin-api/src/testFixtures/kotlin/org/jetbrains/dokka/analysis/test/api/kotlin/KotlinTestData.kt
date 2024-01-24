/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api.kotlin

import org.jetbrains.dokka.analysis.test.api.TestData
import org.jetbrains.dokka.analysis.test.api.TestDataFile

/**
 * A container for populating and holding Kotlin source code test data.
 *
 * This container exists so that common creation, population and verification logic
 * can be reused, instead of having to implement [KtFileCreator] multiple times.
 *
 * @param pathToKotlinSources path to the `src` directory in which Kotlin sources must reside.
 *                            Must be relative to the root of the test project. Example: `/src/main/kotlin`
 * @see TestData
 */
class KotlinTestData(
    private val pathToKotlinSources: String
) : TestData, KtFileCreator {

    private val files = mutableListOf<TestDataFile>()

    override fun ktFile(
        pathFromSrc: String,
        fqPackageName: String,
        fillFile: KotlinTestDataFile.() -> Unit
    ) {
        check(pathFromSrc.endsWith(".kt")) { "Kotlin files are expected to have .kt extension" }

        val testDataFile = KotlinTestDataFile(
            pathFromProjectRoot = "$pathToKotlinSources/$pathFromSrc",
            fqPackageName = fqPackageName,
        )
        fillFile(testDataFile)
        files.add(testDataFile)
    }

    override fun getFiles(): List<TestDataFile> {
        return files
    }

    override fun toString(): String {
        return "KotlinTestData(pathToKotlinSources='$pathToKotlinSources', files=$files)"
    }
}
