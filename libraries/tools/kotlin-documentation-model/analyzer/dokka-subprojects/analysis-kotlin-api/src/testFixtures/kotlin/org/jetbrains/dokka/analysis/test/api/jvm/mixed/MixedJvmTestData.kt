/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api.jvm.mixed

import org.jetbrains.dokka.analysis.test.api.TestData
import org.jetbrains.dokka.analysis.test.api.TestDataFile
import org.jetbrains.dokka.analysis.test.api.jvm.java.JavaFileCreator
import org.jetbrains.dokka.analysis.test.api.jvm.java.JavaTestData
import org.jetbrains.dokka.analysis.test.api.jvm.java.JavaTestDataFile
import org.jetbrains.dokka.analysis.test.api.kotlin.KotlinTestData
import org.jetbrains.dokka.analysis.test.api.kotlin.KotlinTestDataFile
import org.jetbrains.dokka.analysis.test.api.kotlin.KtFileCreator
import org.jetbrains.dokka.analysis.test.api.util.AnalysisTestDslMarker

/**
 * A container for populating and holding Kotlin and Java source code test data.
 *
 * This container exists so that common creation, population and verification logic
 * can be reused, instead of having to implement [KtFileCreator] and [JavaFileCreator] multiple times.
 *
 * @see TestData
 */
class MixedJvmTestData(pathToSources: String) : TestData, KtFileCreator, JavaFileCreator {

    private val javaTestData = JavaTestData(pathToJavaSources = pathToSources)
    private val kotlinJvmTestData = KotlinTestData(pathToKotlinSources = pathToSources)

    @AnalysisTestDslMarker
    override fun javaFile(pathFromSrc: String, fillFile: JavaTestDataFile.() -> Unit) {
        javaTestData.javaFile(pathFromSrc, fillFile)
    }

    @AnalysisTestDslMarker
    override fun ktFile(pathFromSrc: String, fqPackageName: String, fillFile: KotlinTestDataFile.() -> Unit) {
        kotlinJvmTestData.ktFile(pathFromSrc, fqPackageName, fillFile)
    }

    override fun getFiles(): List<TestDataFile> {
        return javaTestData.getFiles() + kotlinJvmTestData.getFiles()
    }

    override fun toString(): String {
        return "MixedJvmTestData(javaTestData=$javaTestData, kotlinJvmTestData=$kotlinJvmTestData)"
    }
}
