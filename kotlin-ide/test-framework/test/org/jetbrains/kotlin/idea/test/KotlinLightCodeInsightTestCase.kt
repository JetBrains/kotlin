/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused", "IncompatibleAPI", "PropertyName")

package org.jetbrains.kotlin.idea.test

import org.jetbrains.kotlin.test.KotlinTestUtils.*
import java.io.File

@Suppress("DEPRECATION")
@Deprecated("Use KotlinLightCodeInsightFixtureTestCase instead")
abstract class KotlinLightCodeInsightTestCase : com.intellij.testFramework.LightCodeInsightTestCase() {
    override fun getTestDataPath(): String {
        val clazz = this::class.java
        val root = getTestsRoot(clazz)
        val path = if (filesBasedTest) {
            File(root)
        } else {
            val test = getTestDataFileName(clazz, name) ?: error("No @TestMetadata for ${clazz.name}")
            File(root, test)
        }
        return toSlashEndingDirPath(path.path)
    }

    protected open val filesBasedTest: Boolean = false

    protected fun testDataFile(fileName: String): File = File(testDataPath, fileName)

    protected fun testDataFile(): File = testDataFile(fileName())

    protected fun testPath(fileName: String = fileName()): String = testDataFile(fileName).toString()

    protected fun testPath(): String = testPath(fileName())

    protected open fun fileName(): String = getTestDataFileName(this::class.java, this.name) ?: (getTestName(false) + ".kt")

}