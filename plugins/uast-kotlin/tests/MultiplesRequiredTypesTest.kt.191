/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.test.kotlin

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.uast.*
import org.jetbrains.uast.test.env.kotlin.assertEqualsToFile
import java.io.File

class MultiplesRequiredTypesTest : AbstractKotlinUastTest() {


    fun testInnerClasses() = doTest("InnerClasses")

    override fun check(testName: String, file: UFile) {
        val valuesFile = getTestFile(testName, "splog.txt")
        assertEqualsToFile("MultiplesTargetConversionResult", valuesFile, file.asMultiplesTargetConversionResult())
    }

    private fun UFile.asMultiplesTargetConversionResult(): String {
        val plugin = UastLanguagePlugin.byLanguage(KotlinLanguage.INSTANCE)!!
        val builder = StringBuilder()
        var level = 0
        (this.psi as KtFile).accept(object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                val uElement =
                    plugin.convertElementWithParent(
                        element,
                        arrayOf(UFile::class.java, UClass::class.java, UField::class.java, UMethod::class.java)
                    )
                if (uElement != null) {
                    builder.append("    ".repeat(level))
                    builder.append(uElement.asLogString())
                    builder.appendln()
                }
                if (uElement != null) level++
                element.acceptChildren(this)
                if (uElement != null) level--
            }
        })
        return builder.toString()
    }

    private fun getTestFile(testName: String, ext: String) =
        File(File(AbstractKotlinUastTest.TEST_KOTLIN_MODEL_DIR, testName).canonicalPath + '.' + ext)

}
