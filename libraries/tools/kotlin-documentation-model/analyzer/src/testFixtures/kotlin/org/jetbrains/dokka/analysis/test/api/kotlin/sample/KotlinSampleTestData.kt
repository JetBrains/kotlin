/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api.kotlin.sample

import org.jetbrains.dokka.analysis.test.api.TestData
import org.jetbrains.dokka.analysis.test.api.TestDataFile

/**
 * A container for populating and holding Kotlin sample test data.
 *
 * This container exists so that common creation, population and verification logic
 * can be reused, instead of having to implement [KotlinSampleFileCreator] multiple times.
 *
 * @see TestData
 */
class KotlinSampleTestData : TestData, KotlinSampleFileCreator {

    private val files = mutableListOf<TestDataFile>()

    override fun sampleFile(
        pathFromProjectRoot: String,
        fqPackageName: String,
        fillFile: KotlinSampleTestDataFile.() -> Unit
    ) {
        check(pathFromProjectRoot.endsWith(".kt")) { "Kotlin sample files are expected to have .kt extension" }

        val testDataFile = KotlinSampleTestDataFile(
            pathFromProjectRoot = pathFromProjectRoot,
            fqPackageName = fqPackageName
        )
        fillFile(testDataFile)
        files.add(testDataFile)
    }

    override fun getFiles(): List<TestDataFile> {
        return files
    }

    override fun toString(): String {
        return "KotlinSampleTestData(files=$files)"
    }
}
