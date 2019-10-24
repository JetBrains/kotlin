/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.resolve

import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.idea.decompiler.classFile.KtClsFile
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.SdkAndMockLibraryProjectDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.junit.Assert
import java.io.File

private val FILE_WITH_KOTLIN_CODE = PluginTestCaseBase.TEST_DATA_DIR + "/resolve/referenceInJava/dependency/dependencies.kt"

abstract class AbstractReferenceResolveInJavaTest : AbstractReferenceResolveTest() {
    override fun doTest(path: String) {
        val fileName = fileName()
        assert(fileName.endsWith(".java")) { fileName }
        myFixture.configureByText("dependencies.kt", FileUtil.loadFile(File(FILE_WITH_KOTLIN_CODE), true))
        myFixture.configureByFile(fileName)
        performChecks()
    }
}

abstract class AbstractReferenceToCompiledKotlinResolveInJavaTest : AbstractReferenceResolveTest() {
    override fun doTest(path: String) {
        myFixture.configureByFile(fileName())
        performChecks()
    }

    override fun getProjectDescriptor() = SdkAndMockLibraryProjectDescriptor(FILE_WITH_KOTLIN_CODE, true)

    override val refMarkerText: String
        get() = "CLS_REF"

    override fun checkResolvedTo(element: PsiElement) {
        val navigationElement = element.navigationElement
        Assert.assertFalse(
            "Reference should not navigate to a light element\nWas: ${navigationElement::class.java.simpleName}",
            navigationElement is KtLightElement<*, *>
        )
        Assert.assertTrue(
            "Reference should navigate to a kotlin declaration\nWas: ${navigationElement::class.java.simpleName}",
            navigationElement is KtDeclaration || navigationElement is KtClsFile
        )
    }
}
