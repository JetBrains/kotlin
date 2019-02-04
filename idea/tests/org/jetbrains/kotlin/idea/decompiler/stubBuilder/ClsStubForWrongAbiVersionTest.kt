/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.decompiler.stubBuilder

import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.decompiler.textBuilder.findTestLibraryRoot
import org.jetbrains.kotlin.idea.test.KotlinJdkAndLibraryProjectDescriptor
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

class ClsStubBuilderForWrongAbiVersionTest : AbstractClsStubBuilderTest() {

    fun testPackage() = testStubsForFileWithWrongAbiVersion("Wrong_packageKt")

    fun testClass() = testStubsForFileWithWrongAbiVersion("ClassWithWrongAbiVersion")

    private fun testStubsForFileWithWrongAbiVersion(className: String) {
        val root = findTestLibraryRoot(myModule!!)!!
        val result = root.findClassFileByName(className)
        testClsStubsForFile(result, null)
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return KotlinJdkAndLibraryProjectDescriptor(File(KotlinTestUtils.getTestDataPathBase() + "/cli/jvm/wrongAbiVersionLib/bin"))
    }
}
