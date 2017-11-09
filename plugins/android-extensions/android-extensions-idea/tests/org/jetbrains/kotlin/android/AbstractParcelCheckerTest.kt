/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.android

import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import java.io.File

abstract class AbstractParcelCheckerTest : KotlinAndroidTestCase() {
    override fun setUp() {
        super.setUp()
        ConfigLibraryUtil.configureKotlinRuntime(myModule)
    }

    override fun tearDown() {
        ConfigLibraryUtil.unConfigureKotlinRuntime(myModule)
        super.tearDown()
    }

    fun doTest(filename: String) {
        myFixture.copyDirectoryToProject("plugins/android-extensions/android-extensions-runtime/src", "src/androidExtensionsRuntime")

        val ktFile = File(filename)
        val virtualFile = myFixture.copyFileToProject(ktFile.absolutePath, "src/" + getTestName(true) + ".kt")
        myFixture.configureFromExistingVirtualFile(virtualFile)

        myFixture.checkHighlighting(true, false, true)
    }
}