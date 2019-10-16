/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.navigation

import com.intellij.openapi.projectRoots.Sdk
import com.intellij.testFramework.LightCodeInsightTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.invalidateLibraryCache
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractKotlinGotoImplementationTest : LightCodeInsightTestCase() {

    override fun setUp() {
        super.setUp()
        invalidateLibraryCache(project)
    }

    override fun getTestDataPath(): String = KotlinTestUtils.getHomeDirectory() + File.separator

    override fun getProjectJDK(): Sdk = PluginTestCaseBase.mockJdk()

    protected fun doTest(path: String) {
        configureByFile(path)
        val gotoData = NavigationTestUtils.invokeGotoImplementations(editor, file)
        NavigationTestUtils.assertGotoDataMatching(editor, gotoData)
    }
}