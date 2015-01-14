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

import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.JdkAndMockLibraryProjectDescriptor
import org.jetbrains.kotlin.idea.PluginTestCaseBase
import com.intellij.psi.ClassFileViewProvider
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.KotlinSyntheticClass.Kind.*

public class InternalCompiledClassesTest : AbstractInternalCompiledClassesTest() {
    private val TEST_DATA_PATH = PluginTestCaseBase.getTestDataPathBase() + "/decompiler/internalClasses"

    fun testPackagePartIsInvisible() = doTestNoPsiFilesAreBuiltForSyntheticClass(PACKAGE_PART)

    fun testSamWrapperIsInvisible() = doTestNoPsiFilesAreBuiltForSyntheticClass(SAM_WRAPPER)

    fun testSamLambdaIsInvisible() = doTestNoPsiFilesAreBuiltForSyntheticClass(SAM_LAMBDA)

    fun testCallableReferenceWrapperIsInvisible() = doTestNoPsiFilesAreBuiltForSyntheticClass(CALLABLE_REFERENCE_WRAPPER)

    fun testLocalFunctionIsInvisible() = doTestNoPsiFilesAreBuiltForSyntheticClass(LOCAL_FUNCTION)

    fun testAnonymousFunctionIsInvisible() = doTestNoPsiFilesAreBuiltForSyntheticClass(ANONYMOUS_FUNCTION)

    fun testLocalClassIsInvisible() = doTestNoPsiFilesAreBuiltForSyntheticClass(LOCAL_CLASS)

    fun testAnonymousObjectIsInvisible() = doTestNoPsiFilesAreBuiltForSyntheticClass(ANONYMOUS_OBJECT)

    fun testInnerClassIsInvisible() = doTestNoPsiFilesAreBuiltFor("inner or nested class") {
        ClassFileViewProvider.isInnerClass(this)
    }

    fun testTraitImplClassIsVisibleAsJavaClass() = doTestTraitImplClassIsVisibleAsJavaClass()

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return JdkAndMockLibraryProjectDescriptor(TEST_DATA_PATH, /* withSources = */ false)
    }
}
