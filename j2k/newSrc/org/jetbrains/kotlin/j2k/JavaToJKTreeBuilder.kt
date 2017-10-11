/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.j2k

import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.*
import org.jetbrains.kotlin.j2k.tree.JKClass
import org.jetbrains.kotlin.j2k.tree.JKElement
import org.jetbrains.kotlin.j2k.tree.JKExpression
import org.jetbrains.kotlin.j2k.tree.impl.JKClassImpl
import org.jetbrains.kotlin.j2k.tree.impl.JKElementBase
import org.jetbrains.kotlin.j2k.tree.impl.JKJavaFieldImpl

class JavaToJKTreeBuilder {

    private class ExpressionTreeVisitor : JavaElementVisitor() {
        override fun visitElement(element: PsiElement) {
            element.acceptChildren(this)
        }

        fun extractExpression(): JKExpression? = object : JKElementBase(), JKExpression {}
    }

    private class ElementVisitor : JavaElementVisitor() {

        override fun visitElement(element: PsiElement) {
            element.acceptChildren(this)
        }

        var currentClass: JKClass? = null

        override fun visitClass(aClass: PsiClass) {
            currentClass = JKClassImpl()
            super.visitClass(aClass)
        }

        override fun visitField(field: PsiField) {
            val initializer = ExpressionTreeVisitor().also { field.initializer?.accept(it) }.extractExpression()

            currentClass!!.declarations += JKJavaFieldImpl(field.name!!, initializer)
            super.visitField(field)
        }

    }

    fun buildTree(psi: PsiElement): JKElement? {
        assert(psi.language.`is`(JavaLanguage.INSTANCE)) { "Unable to build JK Tree using Java Visitor for language ${psi.language}" }
        val elementVisitor = ElementVisitor()
        psi.accept(elementVisitor)
        return elementVisitor.currentClass
    }
}