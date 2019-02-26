/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.test.kotlin

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.test.common.kotlin.IndentedPrintingVisitor
import org.jetbrains.uast.test.common.kotlin.visitUFileAndGetResult
import org.jetbrains.uast.test.env.kotlin.assertEqualsToFile
import org.jetbrains.uast.toUElementOfType
import java.io.File


abstract class AbstractKotlinResolveEverythingTest : AbstractKotlinUastTest() {

    private fun getTestFile(testName: String, ext: String) =
        File(File(AbstractKotlinUastTest.TEST_KOTLIN_MODEL_DIR, testName).canonicalPath + '.' + ext)


    private fun UFile.resolvableWithTargets() = object : IndentedPrintingVisitor(KtBlockExpression::class) {
        override fun render(element: PsiElement) = element.toUElementOfType<UReferenceExpression>()?.let { ref ->
            StringBuilder().apply {
                val parent = ref.uastParent
                append(parent?.asLogString())
                append(" -> ")
                append(ref.asLogString())
                append(" -> ")
                append(ref.resolve())
                append(": ")
                append(ref.resolvedName)
            }
        }
    }.visitUFileAndGetResult(this)

    override fun check(testName: String, file: UFile) {
        assertEqualsToFile("resolved", getTestFile(testName, "resolved.txt"), file.resolvableWithTargets())
    }
}