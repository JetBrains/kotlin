/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k

import com.intellij.psi.*
import org.jetbrains.kotlin.j2k.tree.JKClass
import org.jetbrains.kotlin.j2k.tree.JKDeclaration
import org.jetbrains.kotlin.j2k.tree.JKField
import org.jetbrains.kotlin.j2k.tree.JKMethod
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

    inline fun <reified T : JKSymbol> provideSymbol(reference: PsiReference): T {
        val target = reference.resolve()
        if (target != null) return provideDirectSymbol(target) as T
        return JKUnresolvedField(reference).let { if (it is T) it else JKUnresolvedMethod(reference) as T }
    }

    fun provideUniverseSymbol(psi: PsiElement, jk: JKDeclaration? = null): JKSymbol = symbols.getOrPut(psi) {
        when (psi) {
            is PsiField, is PsiParameter, is PsiLocalVariable -> JKUniverseFieldSymbol()
            is PsiMethod -> JKUniverseMethodSymbol()
            is PsiClass -> JKUniverseClassSymbol()
            else -> TODO()
        }
    }.also {
        if (jk != null)
            when (it) {
                is JKUniverseClassSymbol -> it.target = jk as JKClass
                is JKUniverseFieldSymbol -> it.target = jk as JKField
                is JKUniverseMethodSymbol -> it.target = jk as JKMethod
            }
    }

    private inner class ElementVisitor : JavaElementVisitor() {
        override fun visitClass(aClass: PsiClass) {
            provideUniverseSymbol(aClass)
            aClass.acceptChildren(this)
        }

        override fun visitField(field: PsiField) {
            provideUniverseSymbol(field)
        }

        override fun visitMethod(method: PsiMethod) {
            provideUniverseSymbol(method)
        }

        override fun visitFile(file: PsiFile) {
            file.acceptChildren(this)
        }
    }
}