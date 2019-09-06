/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.decompiler.stubBuilder

import com.intellij.openapi.module.Module
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.decompiler.textBuilder.findTestLibraryRoot
import org.jetbrains.kotlin.idea.test.KotlinJdkAndLibraryProjectDescriptor
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.runner.RunWith
import java.io.File

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class ClsStubBuilderForWrongAbiVersionTest : AbstractClsStubBuilderTest() {
    val module: Module get() = myModule

    fun testPackage() = testStubsForFileWithWrongAbiVersion("Wrong_packageKt")

    fun testClass() = testStubsForFileWithWrongAbiVersion("ClassWithWrongAbiVersion")

    private fun testStubsForFileWithWrongAbiVersion(className: String) {
        val root = findTestLibraryRoot(module!!)!!
        val result = root.findClassFileByName(className)
        testClsStubsForFile(result, null)
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return KotlinJdkAndLibraryProjectDescriptor(File(KotlinTestUtils.getTestDataPathBase() + "/cli/jvm/wrongAbiVersionLib/bin"))
    }
}
