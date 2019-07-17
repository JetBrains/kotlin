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
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.psi
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

fun JKSymbol.fqNameToImport(): String? =
    when {
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


val JKMethodSymbol.parameterNames: List<String>?
    get() {
        return when (this) {
            is JKMultiverseFunctionSymbol -> target.valueParameters.map { it.name ?: return null }
            is JKMultiverseMethodSymbol -> target.parameters.map { it.name ?: return null }
            is JKUniverseMethodSymbol -> target.parameters.map { it.name.value }
            is JKUnresolvedMethod -> null
        }
    }


val JKMethodSymbol.isStatic: Boolean
    get() = when (this) {
        is JKMultiverseFunctionSymbol -> target.parent is KtObjectDeclaration
        is JKMultiverseMethodSymbol -> target.hasModifierProperty(PsiModifier.STATIC)
        is JKUniverseMethodSymbol -> target.parent?.parent?.safeAs<JKClass>()?.classKind == JKClass.ClassKind.COMPANION
        is JKUnresolvedMethod -> false
    }


fun JKMethodSymbol.parameterTypesWithUnfoldedVarargs(): Sequence<JKType>? {
    val realParameterTypes = parameterTypes ?: return null
    if (realParameterTypes.isEmpty()) return emptySequence()
    val lastArrayType = realParameterTypes.last().arrayInnerType() ?: return realParameterTypes.asSequence()
    return realParameterTypes.dropLast(1).asSequence() + generateSequence { lastArrayType }
}

