/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.test.kotlin

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.uast.*
import org.jetbrains.uast.test.common.kotlin.IndentedPrintingVisitor
import org.jetbrains.uast.test.common.kotlin.visitUFileAndGetResult
import org.jetbrains.uast.test.env.kotlin.assertEqualsToFile
import java.io.File


interface ResolveEverythingTestBase {

    fun getTestFile(testName: String, ext: String) =
        File(File(TEST_KOTLIN_MODEL_DIR, testName).canonicalPath + '.' + ext)

    fun UFile.resolvableWithTargets() = object : IndentedPrintingVisitor(KtBlockExpression::class) {
        override fun render(element: PsiElement) =
            UastFacade.convertToAlternatives<UExpression>(element, arrayOf(UReferenceExpression::class.java, UCallExpression::class.java))
                .filter {
                    when (it) {
                        is UCallExpression -> it.sourcePsi.safeAs<KtCallElement>()?.calleeExpression !is KtSimpleNameExpression
                        else -> true
                    }
                }.takeIf { it.any() }
                ?.joinTo(StringBuilder(), "\n") { ref ->
                    StringBuilder().apply {
                        val parent = ref.uastParent
                        append(parent?.asLogString())
                        if (parent is UCallExpression) {
                            append("(resolves to ${parent.resolve()})")
                        }
                        append(" -> ")
                        append(ref.asLogString())
                        append(" -> ")
                        append(ref.cast<UResolvable>().resolve())
                        append(": ")
                        append(
                            when (ref) {
                                is UReferenceExpression -> ref.resolvedName
                                is UCallExpression -> ""
                                else -> "<none>"
                            }
                        )
            }
        }
    }.visitUFileAndGetResult(this)

    fun check(testName: String, file: UFile) {
        assertEqualsToFile("resolved", getTestFile(testName, "resolved.txt"), file.resolvableWithTargets())
    }
}