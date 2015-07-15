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

package org.jetbrains.kotlin.idea.decompiler.stubBuilder

import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.test.JetJdkAndLibraryProjectDescriptor
import java.io.File
import org.jetbrains.kotlin.idea.decompiler.navigation.NavigateToDecompiledLibraryTest
import org.jetbrains.kotlin.test.JetTestUtils

class ClsStubBuilderForWrongAbiVersionTest : AbstractClsStubBuilderTest() {

    fun testPackage() = testStubsForFileWithWrongAbiVersion("WrongPackage")

    fun testClass() = testStubsForFileWithWrongAbiVersion("ClassWithWrongAbiVersion")

    private fun testStubsForFileWithWrongAbiVersion(className: String) {
        val root = NavigateToDecompiledLibraryTest.findTestLibraryRoot(myModule!!)!!
        val result = root.findClassFileByName(className)
        testClsStubsForFile(result, null)
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return JetJdkAndLibraryProjectDescriptor(File(JetTestUtils.getTestDataPathBase() + "/cli/jvm/wrongAbiVersionLib/bin"))
    }
}
