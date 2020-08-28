/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.decompiler.navigation

import com.intellij.testFramework.LightProjectDescriptor
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.SdkAndMockLibraryProjectDescriptor
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.jetbrains.kotlin.test.WithMutedInDatabaseRunTest
import org.jetbrains.kotlin.test.runTest
import org.junit.runner.RunWith

@WithMutedInDatabaseRunTest
@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class NavigateFromJSLibrarySourcesTest : AbstractNavigateFromLibrarySourcesTest() {
    fun testIcon() {
        runTest {
            TestCase.assertEquals(
                "Icon.kt",
                navigationElementForReferenceInLibrarySource("lib.kt", "Icon").containingFile.name
            )
        }
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
