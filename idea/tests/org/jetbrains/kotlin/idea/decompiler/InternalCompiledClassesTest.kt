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

package org.jetbrains.kotlin.idea.decompiler

import com.intellij.psi.ClassFileViewProvider
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.test.JdkAndMockLibraryProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase

class InternalCompiledClassesTest : AbstractInternalCompiledClassesTest() {
    private val TEST_DATA_PATH = PluginTestCaseBase.getTestDataPathBase() + "/decompiler/internalClasses"

    fun testSyntheticClassesAreInvisible() = doTestNoPsiFilesAreBuiltForSyntheticClasses()

    fun testLocalClassesAreInvisible() = doTestNoPsiFilesAreBuiltForLocalClass()

    fun testInnerClassIsInvisible() = doTestNoPsiFilesAreBuiltFor("inner or nested class") {
        ClassFileViewProvider.isInnerClass(this)
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return JdkAndMockLibraryProjectDescriptor(TEST_DATA_PATH, /* withSources = */ false)
    }
}
