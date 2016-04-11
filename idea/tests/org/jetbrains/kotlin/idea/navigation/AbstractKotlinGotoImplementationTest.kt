/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.navigation

import com.intellij.openapi.projectRoots.Sdk
import com.intellij.testFramework.LightCodeInsightTestCase
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.invalidateLibraryCache
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractKotlinGotoImplementationTest : LightCodeInsightTestCase() {

    override fun setUp() {
        super.setUp()
        invalidateLibraryCache(getProject())
    }

    override fun getTestDataPath(): String = KotlinTestUtils.getHomeDirectory() + File.separator

    override fun getProjectJDK(): Sdk = PluginTestCaseBase.mockJdk()

    protected fun doTest(path: String) {
        configureByFile(path)
        val gotoData = NavigationTestUtils.invokeGotoImplementations(LightPlatformCodeInsightTestCase.getEditor(), LightPlatformCodeInsightTestCase.getFile())
        NavigationTestUtils.assertGotoDataMatching(LightPlatformCodeInsightTestCase.getEditor(), gotoData)
    }
}