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

package org.jetbrains.kotlin.search

import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub
import com.intellij.psi.search.searches.AnnotatedMembersSearch
import com.intellij.testFramework.LightProjectDescriptor
import junit.framework.TestCase
import org.jetbrains.kotlin.asJava.builder.LightClassConstructionContext
import org.jetbrains.kotlin.asJava.builder.StubComputationTracker
import org.jetbrains.kotlin.idea.caches.resolve.lightClasses.IDELightClassConstructionContext
import org.jetbrains.kotlin.idea.caches.resolve.lightClasses.IDELightClassConstructionContext.Mode.EXACT
import org.jetbrains.kotlin.idea.completion.test.withServiceRegistered
import org.jetbrains.kotlin.idea.search.PsiBasedClassResolver
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.junit.Assert
import java.io.File

abstract class AbstractAnnotatedMembersSearchTest : AbstractSearcherTest() {
    override fun getProjectDescriptor(): LightProjectDescriptor {
        return KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
    }

    fun doTest(path: String) {
        myFixture.configureByFile(path)
        val fileText = FileUtil.loadFile(File(path), true)
        val directives = InTextDirectivesUtils.findListWithPrefixes(fileText, "// ANNOTATION: ")

        TestCase.assertFalse("Specify ANNOTATION directive in test file", directives.isEmpty())

        val annotationClassName = directives.first()
        project.withServiceRegistered<StubComputationTracker, Unit>(NoRealDelegatesComputed) {
            val psiClass = getPsiClass(annotationClassName)
            PsiBasedClassResolver.trueHits.set(0)
            PsiBasedClassResolver.falseHits.set(0)

            AbstractSearcherTest.checkResult(path, AnnotatedMembersSearch.search(psiClass, projectScope))

            val optimizedTrue = InTextDirectivesUtils.getPrefixedInt(fileText, "// OPTIMIZED_TRUE:")
            if (optimizedTrue != null) {
                TestCase.assertEquals(optimizedTrue.toInt(), PsiBasedClassResolver.trueHits.get())
            }
            val optimizedFalse = InTextDirectivesUtils.getPrefixedInt(fileText, "// OPTIMIZED_FALSE:")
            if (optimizedFalse != null) {
                TestCase.assertEquals(optimizedFalse.toInt(), PsiBasedClassResolver.falseHits.get())
            }
        }

    }

    override fun getTestDataPath(): String {
        return File(PluginTestCaseBase.getTestDataPathBase(), "/search/annotations").path + File.separator
    }
}

private object NoRealDelegatesComputed : StubComputationTracker {
    override fun onStubComputed(javaFileStub: PsiJavaFileStub, context: LightClassConstructionContext) {
        if ((context as IDELightClassConstructionContext).mode == EXACT) {
            Assert.fail("Should not have computed exact delegate for ${javaFileStub.classes.single().qualifiedName}")
        }
    }
}
