/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.test.kotlin

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.uast.*
import org.junit.Test

class KotlinUastAlternativesTest : AbstractKotlinUastTest() {
    override fun check(testName: String, file: UFile) {
    }

    @Test
    fun testPropertyAlternatives() {
        doTest("ManyAlternatives") { name, file ->
            val index = file.psi.text.indexOf("writebleProp")
            val ktProperty = PsiTreeUtil.getParentOfType(file.psi.findElementAt(index), KtProperty::class.java)!!
            val plugin = UastLanguagePlugin.byLanguage(ktProperty.language)!!

            plugin.convertToAlternatives<UElement>(ktProperty, arrayOf(UField::class.java)).let {
                assertEquals(
                    "@org.jetbrains.annotations.NotNull private var writebleProp: int = 0",
                    it.joinToString(transform = UElement::asRenderString)
                )
            }

            plugin.convertToAlternatives<UElement>(ktProperty, arrayOf(UMethod::class.java, UField::class.java)).let {
                assertEquals(
                    "public final fun getWritebleProp() : int = UastEmptyExpression, " +
                            "public final fun setWritebleProp(@null p: int) : void = UastEmptyExpression, " +
                            "@org.jetbrains.annotations.NotNull private var writebleProp: int = 0",
                    it.joinToString(transform = UElement::asRenderString)
                )
            }

            plugin.convertToAlternatives<UElement>(ktProperty, arrayOf(UField::class.java, UMethod::class.java)).let {
                assertEquals(
                    "@org.jetbrains.annotations.NotNull private var writebleProp: int = 0, " +
                            "public final fun getWritebleProp() : int = UastEmptyExpression, " +
                            "public final fun setWritebleProp(@null p: int) : void = UastEmptyExpression",
                    it.joinToString(transform = UElement::asRenderString)
                )
            }

            plugin.convertToAlternatives<UElement>(ktProperty, arrayOf(UDeclaration::class.java)).let {
                assertEquals(
                    "@org.jetbrains.annotations.NotNull private var writebleProp: int = 0, " +
                            "public final fun getWritebleProp() : int = UastEmptyExpression, " +
                            "public final fun setWritebleProp(@null p: int) : void = UastEmptyExpression",
                    it.joinToString(transform = UElement::asRenderString)
                )
            }

        }
    }

}