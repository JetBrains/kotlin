/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.symbols

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.search.declarationsSearch.findDeepestSuperMethodsNoWrapping
import org.jetbrains.kotlin.nj2k.psi
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.types.JKType
import org.jetbrains.kotlin.nj2k.types.arrayInnerType

import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


val JKSymbol.isUnresolved
    get() = this is JKUnresolvedSymbol

fun JKSymbol.getDisplayName(): String {
    if (this !is JKUniverseSymbol<*>) return fqName
    return generateSequence(declaredIn as? JKUniverseClassSymbol) { symbol ->
        symbol.declaredIn.safeAs<JKUniverseClassSymbol>()?.takeIf { !it.target.hasOtherModifier(OtherModifier.INNER) }
    }.fold(name) { acc, symbol -> "${symbol.name}.$acc" }
}

fun JKSymbol.fqNameToImport(): String? = when {
    this is JKClassSymbol && this !is JKUniverseClassSymbol -> fqName
    else -> null
}

fun JKSymbol.deepestFqName(): String? {
    fun Any.deepestFqNameForTarget(): String? =
        when (this) {
            is PsiMethod -> (findDeepestSuperMethods().firstOrNull() ?: this).getKotlinFqName()?.asString()
            is KtNamedFunction -> findDeepestSuperMethodsNoWrapping(this).firstOrNull()?.getKotlinFqName()?.asString()
            is JKMethod -> psi<PsiElement>()?.deepestFqNameForTarget()
            else -> null
        }
    return target.deepestFqNameForTarget() ?: fqName
}
