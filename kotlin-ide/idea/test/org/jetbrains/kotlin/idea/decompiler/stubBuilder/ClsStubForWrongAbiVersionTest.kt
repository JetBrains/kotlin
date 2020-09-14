/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.decompiler.stubBuilder

import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.decompiler.textBuilder.findTestLibraryRoot
import org.jetbrains.kotlin.idea.test.IDEA_TEST_DATA_DIR
import org.jetbrains.kotlin.idea.test.KotlinJdkAndLibraryProjectDescriptor
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.RunWith
import java.io.File

@RunWith(JUnit38ClassRunner::class)
class ClsStubBuilderForWrongAbiVersionTest : AbstractClsStubBuilderTest() {

    fun testPackage() = testStubsForFileWithWrongAbiVersion("Wrong_packageKt")

    fun testClass() = testStubsForFileWithWrongAbiVersion("ClassWithWrongAbiVersion")

    private fun testStubsForFileWithWrongAbiVersion(className: String) {
        val root = findTestLibraryRoot(module!!)!!
        val result = root.findClassFileByName(className)
        testClsStubsForFile(result, null)
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return KotlinJdkAndLibraryProjectDescriptor(File(IDEA_TEST_DATA_DIR.absolutePath + "/wrongAbiVersionLib/bin"))
    }
}
