/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.symbols

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.nj2k.JKSymbolProvider
import org.jetbrains.kotlin.nj2k.tree.JKDeclaration
import org.jetbrains.kotlin.nj2k.tree.JKFile
import org.jetbrains.kotlin.nj2k.tree.parentOfType
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

interface JKSymbol {
    val target: Any
    val declaredIn: JKSymbol?
    val fqName: String
    val name: String
}

interface JKUniverseSymbol<T : JKDeclaration> : JKSymbol {
    val symbolProvider: JKSymbolProvider
    override var target: T
    override val fqName: String
        get() {
            val qualifier =
                declaredIn?.fqName
                    ?: target
                        .parentOfType<JKFile>()
                        ?.packageDeclaration
                        ?.name
                        ?.value
            return qualifier?.takeIf { it.isNotBlank() }?.let { "$it." }.orEmpty() + name
        }

    override val name: String
        get() = target.name.value

    override val declaredIn: JKSymbol?
        get() = target.parentOfType<JKDeclaration>()?.let { symbolProvider.symbolsByJK[it] }
}

interface JKMultiverseSymbol<T> : JKSymbol where T : PsiNamedElement, T : PsiElement {
    val symbolProvider: JKSymbolProvider

    override val target: T
    override val declaredIn: JKSymbol?
        get() = target.getStrictParentOfType<PsiMember>()?.let { symbolProvider.provideDirectSymbol(it) }
    override val fqName: String
        get() = target.getKotlinFqName()?.asString() ?: name
    override val name: String
        get() = target.name!!
}

interface JKMultiverseKtSymbol<T : KtNamedDeclaration> : JKSymbol {
    val symbolProvider: JKSymbolProvider

    override val target: T
    override val name: String
        get() = target.name!!
    override val declaredIn: JKSymbol?
        get() = target.getStrictParentOfType<KtDeclaration>()?.let { symbolProvider.provideDirectSymbol(it) }
    override val fqName: String
        get() = target.fqName?.asString() ?: name
}

interface JKUnresolvedSymbol : JKSymbol {
    override val target: String
    override val declaredIn: JKSymbol?
        get() = null
    override val fqName: String
        get() = target
    override val name: String
        get() = target.substringAfterLast(".")
}

