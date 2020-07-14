/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.decompiler.navigation

import com.intellij.testFramework.LightProjectDescriptor
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.framework.KotlinSdkType
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.MockLibraryFacility
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.KotlinCompilerStandalone
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.RunWith
import java.io.File

@RunWith(JUnit38ClassRunner::class)
class NavigateFromJSLibrarySourcesTest : AbstractNavigateFromLibrarySourcesTest() {
    private val mockLibraryFacility = MockLibraryFacility(
        source = File(PluginTestCaseBase.getTestDataPathBase(), "decompiler/navigation/fromJSLibSource"),
        platform = KotlinCompilerStandalone.Platform.JavaScript(MockLibraryFacility.MOCK_LIBRARY_NAME, "lib")
    )

    fun testIcon() {
        TestCase.assertEquals(
            "Icon.kt",
            navigationElementForReferenceInLibrarySource("lib.kt", "Icon").containingFile.name
        )
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return object : KotlinLightProjectDescriptor() {
            override fun getSdk() = KotlinSdkType.INSTANCE.createSdkWithUniqueName(emptyList())
        }
    }

    override fun setUp() {
        super.setUp()
        mockLibraryFacility.setUp(module)
    }

    override fun tearDown() {
        mockLibraryFacility.tearDown(module)
        super.tearDown()
    }
}
