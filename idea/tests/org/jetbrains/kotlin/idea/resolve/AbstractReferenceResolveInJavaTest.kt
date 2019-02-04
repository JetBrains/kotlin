/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.resolve

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.idea.decompiler.classFile.KtClsFile
import org.jetbrains.kotlin.idea.test.SdkAndMockLibraryProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.KtDeclaration
import org.junit.Assert

private val FILE_WITH_KOTLIN_CODE = PluginTestCaseBase.TEST_DATA_DIR + "/resolve/referenceInJava/dependency/dependencies.kt"

abstract class AbstractReferenceResolveInJavaTest : AbstractReferenceResolveTest() {
    override fun doTest(path: String) {
        assert(path.endsWith(".java")) { path }
        myFixture.configureByFile(FILE_WITH_KOTLIN_CODE)
        myFixture.configureByFile(path)
        performChecks()
    }
}

abstract class AbstractReferenceToCompiledKotlinResolveInJavaTest : AbstractReferenceResolveTest() {
    override fun doTest(path: String) {
        myFixture.configureByFile(path)
        performChecks()
    }

    override fun getProjectDescriptor() = SdkAndMockLibraryProjectDescriptor(FILE_WITH_KOTLIN_CODE, true)

    override val refMarkerText: String
        get() = "CLS_REF"

    override fun checkResolvedTo(element: PsiElement) {
        val navigationElement = element.navigationElement
        Assert.assertFalse("Reference should not navigate to a light element\nWas: ${navigationElement::class.java.simpleName}", navigationElement is KtLightElement<*, *>)
        Assert.assertTrue("Reference should navigate to a kotlin declaration\nWas: ${navigationElement::class.java.simpleName}", navigationElement is KtDeclaration || navigationElement is KtClsFile)
    }
}
