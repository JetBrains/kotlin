/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.search

import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.testFramework.LightProjectDescriptor
import junit.framework.TestCase
import org.jetbrains.kotlin.asJava.builder.LightClassConstructionContext
import org.jetbrains.kotlin.asJava.builder.StubComputationTracker
import org.jetbrains.kotlin.idea.caches.lightClasses.IDELightClassConstructionContext
import org.jetbrains.kotlin.idea.caches.lightClasses.IDELightClassConstructionContext.Mode.EXACT
import org.jetbrains.kotlin.idea.completion.test.withServiceRegistered
import org.jetbrains.kotlin.idea.search.PsiBasedClassResolver
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.junit.Assert

abstract class AbstractAnnotatedMembersSearchTest : AbstractSearcherTest() {
    override fun getProjectDescriptor(): LightProjectDescriptor {
        return KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
    }

    fun doTest(unused: String) {
        myFixture.configureByFile(fileName())
        val fileText = FileUtil.loadFile(testDataFile(), true)
        val directives = InTextDirectivesUtils.findListWithPrefixes(fileText, "// ANNOTATION: ")

        TestCase.assertFalse("Specify ANNOTATION directive in test file", directives.isEmpty())

        val annotationClassName = directives.first()
        project.withServiceRegistered<StubComputationTracker, Unit>(NoRealDelegatesComputed) {
            val psiClass = getPsiClass(annotationClassName)
            PsiBasedClassResolver.trueHits.set(0)
            PsiBasedClassResolver.falseHits.set(0)

            checkResult(
                testPath(),
                AnnotatedElementsSearch.searchElements(
                    psiClass,
                    projectScope,
                    PsiModifierListOwner::class.java
                )
            )

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

}

private object NoRealDelegatesComputed : StubComputationTracker {
    override fun onStubComputed(javaFileStub: PsiJavaFileStub, context: LightClassConstructionContext) {
        if ((context as IDELightClassConstructionContext).mode == EXACT) {
            Assert.fail("Should not have computed exact delegate for ${javaFileStub.classes.single().qualifiedName}")
        }
    }
}
