/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.test.kotlin

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.uast.DEFAULT_TYPES_LIST
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UastLanguagePlugin
import org.jetbrains.uast.test.env.kotlin.assertEqualsToFile
import java.io.File

class AlternativesRenderLogTest : AbstractKotlinUastTest() {


    fun testClassAnnotation() = doTest("ClassAnnotation")

    fun testInnerClasses() = doTest("InnerClasses")

    fun testLocalDeclarations() = doTest("LocalDeclarations")

    fun testParameterPropertyWithAnnotation() = doTest("ParameterPropertyWithAnnotation")

    override fun check(testName: String, file: UFile) {
        val valuesFile = getTestFile(testName, "altlog.txt")
        assertEqualsToFile("alternatives conversion result", valuesFile, file.asMultiplesTargetConversionResult())
    }

    private fun UFile.asMultiplesTargetConversionResult(): String {
        val plugin = UastLanguagePlugin.byLanguage(KotlinLanguage.INSTANCE)!!
        val builder = StringBuilder()
        var level = 0
        (this.psi as KtFile).accept(object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                val uElement = plugin.convertToAlternatives(element, DEFAULT_TYPES_LIST).toList()

                if (uElement.any()) {
                    builder.append("    ".repeat(level))
                    builder.append("[${uElement.size}]:")
                    builder.append(uElement.joinToString(", ", "[", "]") { it.asLogString() })
                    builder.appendln()
                }
                if (uElement.any()) level++
                element.acceptChildren(this)
                if (uElement.any()) level--
            }
        })
        return builder.toString()
    }

    private fun getTestFile(testName: String, ext: String) =
        File(File(AbstractKotlinUastTest.TEST_KOTLIN_MODEL_DIR, testName).canonicalPath + '.' + ext)

}
