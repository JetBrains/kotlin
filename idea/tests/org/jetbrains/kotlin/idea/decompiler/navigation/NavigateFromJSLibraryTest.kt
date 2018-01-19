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

package org.jetbrains.kotlin.idea.decompiler.navigation

import com.intellij.testFramework.LightProjectDescriptor
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.test.SdkAndMockLibraryProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase

class NavigateFromJSLibrarySourcesTest : AbstractNavigateFromLibrarySourcesTest() {
    fun testIcon() {
        TestCase.assertEquals(
            "Icon.kt",
            navigationElementForReferenceInLibrarySource("lib.kt", "Icon").containingFile.name
        )
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return SdkAndMockLibraryProjectDescriptor(
            PluginTestCaseBase.getTestDataPathBase() + "/decompiler/navigation/fromJSLibSource",
            true,
            true,
            true,
            false
        )
    }
}
