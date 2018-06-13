/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k

import com.intellij.psi.*
import org.jetbrains.kotlin.j2k.tree.JKLocalVariable
import org.jetbrains.kotlin.j2k.tree.JKParameter
import org.jetbrains.kotlin.j2k.tree.impl.*
import org.jetbrains.kotlin.psi.KtClassOrObject


class JKSymbolProvider {
    val symbols = mutableMapOf<PsiElement, JKSymbol>()
    private val elementVisitor = ElementVisitor()

    fun preBuildTree(files: List<PsiJavaFile>) {
        files.forEach { it.accept(elementVisitor) }
    }

    fun provideDirectSymbol(psi: PsiElement): JKSymbol {
        return symbols.getOrPut(psi) {
            when (psi) {
                is PsiClass -> JKMultiverseClassSymbol(psi)
                is KtClassOrObject -> JKMultiverseKtClassSymbol(psi)
                is PsiMethod -> JKMultiverseMethodSymbol(psi)
                is PsiField -> JKMultiverseFieldSymbol(psi)
                else -> TODO(psi::class.toString())
            }
        }
    }

    fun provideSymbol(reference: PsiReference): JKSymbol {
        val target = reference.resolve()
        if (target != null) return provideDirectSymbol(target)
        TODO()
    }

    fun provideLocalVarSymbol(psi: PsiLocalVariable, variable: JKLocalVariable): JKSymbol {
        return symbols.getOrPut(psi) { JKUniverseFieldSymbol(variable) }
    }

    fun provideParameterSymbol(psi: PsiParameter, variable: JKParameter): JKSymbol {
        return symbols.getOrPut(psi) { JKUniverseFieldSymbol(variable) }
    }

    private inner class ElementVisitor : JavaElementVisitor() {
        override fun visitClass(aClass: PsiClass) {
            symbols[aClass] = JKUniverseClassSymbol()
            aClass.acceptChildren(this)
        }

        override fun visitField(field: PsiField) {
            symbols[field] = JKUniverseFieldSymbol()
        }

        override fun visitMethod(method: PsiMethod) {
            symbols[method] = JKUniverseMethodSymbol()
        }

        override fun visitFile(file: PsiFile) {
            file.acceptChildren(this)
        }
    }
}