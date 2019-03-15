/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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
