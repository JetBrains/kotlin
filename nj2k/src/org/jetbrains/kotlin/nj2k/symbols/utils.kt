/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.symbols

import com.intellij.lang.jvm.JvmModifier
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.search.KotlinSearchUsagesSupport.Companion.findDeepestSuperMethodsNoWrapping
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.nj2k.isObjectOrCompanionObject
import org.jetbrains.kotlin.nj2k.psi
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


val JKSymbol.isUnresolved
    get() = this is JKUnresolvedSymbol

fun JKSymbol.getDisplayFqName(): String {
    fun JKSymbol.isDisplayable() = this is JKClassSymbol || this is JKPackageSymbol
    if (this !is JKUniverseSymbol<*>) return fqName
    return generateSequence(declaredIn?.takeIf { it.isDisplayable() }) { symbol ->
        symbol.declaredIn?.takeIf { it.isDisplayable() }
    }.fold(name) { acc, symbol -> "${symbol.name}.$acc" }
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

val JKSymbol.containingClass
    get() = declaredIn as? JKClassSymbol

val JKSymbol.isStaticMember
    get() = when (val target = target) {
        is PsiModifierListOwner -> target.hasModifier(JvmModifier.STATIC)
        is KtElement -> target.getStrictParentOfType<KtClassOrObject>()
            ?.safeAs<KtObjectDeclaration>()
            ?.isCompanion() == true
        is JKTreeElement ->
            target.safeAs<JKOtherModifiersOwner>()?.hasOtherModifier(OtherModifier.STATIC) == true
                    || target.parentOfType<JKClass>()?.isObjectOrCompanionObject == true
        else -> false
    }

val JKSymbol.isEnumConstant
    get() = when (target) {
        is JKEnumConstant -> true
        is PsiEnumConstant -> true
        is KtEnumEntry -> true
        else -> false
    }

val JKSymbol.isUnnamedCompanion
    get() = when (val target = target) {
        is JKClass -> target.classKind == JKClass.ClassKind.COMPANION
        is KtObjectDeclaration -> target.isCompanion() && target.name == SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT.toString()
        else -> false
    }

