/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.idea.frontend.api.types.isUnit
import org.jetbrains.kotlin.idea.util.ifTrue
import org.jetbrains.kotlin.lexer.KtTokens
import java.util.*

internal class FirLightSimpleMethodForSymbol(
    private val functionSymbol: KtFunctionSymbol,
    lightMemberOrigin: LightMemberOrigin?,
    containingClass: FirLightClassBase,
    methodIndex: Int,
    isTopLevel: Boolean,
    argumentsSkipMask: BitSet? = null
) : FirLightMethodForSymbol(
    functionSymbol = functionSymbol,
    lightMemberOrigin = lightMemberOrigin,
    containingClass = containingClass,
    methodIndex = methodIndex,
    argumentsSkipMask = argumentsSkipMask
) {

    private val _name: String by lazyPub {
        functionSymbol.computeJvmMethodName(functionSymbol.name.asString(), containingClass)
    }

    override fun getName(): String = _name

    private val _annotations: List<PsiAnnotation> by lazyPub {

        val needUnknownNullability = functionSymbol.type.isUnit || (_visibility == PsiModifier.PRIVATE)

        val nullability = if (needUnknownNullability) NullabilityType.Unknown else functionSymbol.type.getTypeNullability(
            functionSymbol,
            FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE
        )

        functionSymbol.computeAnnotations(
            parent = this,
            nullability = nullability,
            annotationUseSiteTarget = null,
        )
    }

    private val _visibility: String by lazyPub {
        functionSymbol.isOverride.ifTrue {
            (containingClass as? FirLightClassForSymbol)
                ?.tryGetEffectiveVisibility(functionSymbol)
                ?.toPsiVisibility(isTopLevel)
        } ?: functionSymbol.computeVisibility(isTopLevel = isTopLevel)
    }

    private val _modifiers: Set<String> by lazyPub {

        if (functionSymbol.hasInlineOnlyAnnotation()) return@lazyPub setOf(PsiModifier.FINAL, PsiModifier.PRIVATE)

        val finalModifier = kotlinOrigin?.hasModifier(KtTokens.FINAL_KEYWORD) == true

        val modifiers = functionSymbol.computeModalityForMethod(
            isTopLevel = isTopLevel,
            suppressFinal = !finalModifier && functionSymbol.isOverride
        ) + _visibility

        modifiers.add(
            what = PsiModifier.STATIC,
            `if` = functionSymbol.hasJvmStaticAnnotation()
        )
    }

    private val _modifierList: PsiModifierList by lazyPub {
        FirLightClassModifierList(this, _modifiers, _annotations)
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    override fun isConstructor(): Boolean = false

    private val _returnedType: PsiType by lazyPub {
        if (functionSymbol.type.isUnit) return@lazyPub PsiType.VOID
        functionSymbol.asPsiType(this@FirLightSimpleMethodForSymbol, FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
    }

    override fun getReturnType(): PsiType = _returnedType

    override fun equals(other: Any?): Boolean =
        this === other || (other is FirLightSimpleMethodForSymbol && functionSymbol == other.functionSymbol)

    override fun hashCode(): Int = functionSymbol.hashCode()
}