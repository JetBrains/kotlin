/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.idea.test.MockLibraryFacility
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.test.TestRoot
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.RunWith
import java.io.File

@TestRoot("idea")
@TestDataPath("\$CONTENT_ROOT")
@RunWith(JUnit38ClassRunner::class)
@TestMetadata("testData/codeInsight/overrideImplement/withLib")
class OverrideImplementWithLibTest : AbstractOverrideImplementTest() {
    private companion object {
        private val MOCK_SOURCES_BASE = File(PluginTestCaseBase.getTestDataPathBase(), "codeInsight/overrideImplement/withLib")
    }

    private lateinit var mockLibraryFacility: MockLibraryFacility

    override fun setUp() {
        super.setUp()
        mockLibraryFacility = MockLibraryFacility(File(MOCK_SOURCES_BASE, getTestName(true) + "Src"), attachSources = false)
        mockLibraryFacility.setUp(module)
    }

    override fun tearDown() {
        mockLibraryFacility.tearDown(module)
        super.tearDown()
    }

    fun testFakeOverride() {
        doOverrideFileTest()
    }

    fun testGenericSubstituted() {
        doOverrideFileTest()
    }
}
