/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.nullabilityAnalysis

import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.JKElementInfoLabel
import org.jetbrains.kotlin.nj2k.asLabel
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal class ContextCreator(
    private val conversionContext: NewJ2kConverterContext,
    private val getNullability: (KtTypeElement, NewJ2kConverterContext) -> Nullability
) {

    private fun KtCallableDeclaration.typeElement(): KtTypeElement? =
        typeReference?.typeElement

    private fun KtElement.asTypeVariableOwner(): TypeVariableOwner? =
        when (this) {
            is KtParameter -> typeElement()?.asTypeVariable()?.let { ParameterTarget(this, it) }
            is KtProperty -> typeElement()?.asTypeVariable()?.let { PropertyTarget(this, it) }
            is KtNamedFunction -> typeElement()?.asTypeVariable()?.let { FunctionTarget(this, it) }

            is KtBinaryExpressionWithTypeRHS -> right?.typeElement
                ?.takeIf { KtPsiUtil.isUnsafeCast(this) }
                ?.asTypeVariable()
                ?.let { TypeCastTarget(this, it) }

            is KtCallExpression ->
                if (typeArguments.isNotEmpty())
                    FunctionCallTypeArgumentTarget(
                        this,
                        typeArguments.map { it.typeReference?.typeElement?.asTypeVariable()!! }
                    )
                else null
            else -> null
        }

    fun createContext(analysisScope: AnalysisScope): AnalysisContext {
        val typeVariableOwners = analysisScope.flatMap {
            it.collectDescendantsOfType<KtElement>().mapNotNull { ktElement ->
                ktElement.asTypeVariableOwner()
            }
        }

        val typeElementsToTypeVariables =
            typeVariableOwners.flatMap {
                it.innerTypeVariables() + it.allTypeVariables
            }.associateBy { it.typeElement }

        val declarationToTypeVariable = typeVariableOwners.asSequence()
            .mapNotNull { owner ->
                if (owner is DeclarationTypeVariableOwner)
                    owner.target to owner.typeVariable
                else null
            }.toMap()
        return AnalysisContext(typeVariableOwners, typeElementsToTypeVariables, declarationToTypeVariable)
    }

    private fun KtTypeElement.asTypeVariable(): TypeVariable {
        val nullability = getNullability(this, conversionContext)
        val classReference: ClassReference = classReference()
        val typeParameters: List<TypeVariableTypeParameter> =
            typeArgumentsAsTypes.mapIndexed { index, typeRef ->
                typeRef?.typeElement?.asTypeVariable()?.let { typeVariable ->
                    TypeVariableTypeParameterWithTypeParameter(
                        typeVariable,
                        classReference.typeParametersVariance(index)
                    )
                } ?: TypeVariableStartProjectionTypeParameter

            }
        return TypeVariable(this, classReference, typeParameters, nullability)
    }
}


fun nullabilityByUndefinedNullabilityComment(typeElement: KtTypeElement, context: NewJ2kConverterContext): Nullability {
    val hasUndefinedNullabilityLabel =
        typeElement.parent?.safeAs<KtTypeReference>()?.hasUndefinedNullabilityLabel(context) ?: false
    return when {
        hasUndefinedNullabilityLabel -> Nullability.UNKNOWN
        typeElement is KtNullableType -> Nullability.NULLABLE
        else -> Nullability.NOT_NULL
    }
}


fun PsiElement.getLabel(): JKElementInfoLabel? =
    prevSibling?.safeAs<PsiComment>()?.text?.asLabel()
        ?: parent?.safeAs<KtTypeProjection>()?.getLabel()

private fun KtTypeReference.hasUndefinedNullabilityLabel(context: NewJ2kConverterContext) =
    getLabel()?.let { label ->
        context.elementsInfoStorage.getInfoForLabel(label).orEmpty().contains(org.jetbrains.kotlin.nj2k.UnknownNullability)
    } ?: false

fun prepareTypeElementByMakingAllTypesNullableConsideringNullabilityComment(typeElement: KtTypeElement, context: NewJ2kConverterContext) {
    val hasUndefinedNullabilityLabel =
        typeElement.parent?.safeAs<KtTypeReference>()?.hasUndefinedNullabilityLabel(context) ?: false

    if (hasUndefinedNullabilityLabel) {
        typeElement.changeNullability(toNullable = true)
    }
}

fun prepareTypeElementByMakingAllTypesNullable(typeElement: KtTypeElement, conversionContext: NewJ2kConverterContext) {
    typeElement.changeNullability(toNullable = true)
}


internal data class AnalysisContext(
    val typeVariableOwners: List<TypeVariableOwner>,
    val typeElementToTypeVariable: Map<KtTypeElement, TypeVariable>,
    val declarationToTypeVariable: Map<KtCallableDeclaration, TypeVariable>
)

data class AnalysisScope(val elements: List<PsiElement>) : Iterable<PsiElement> by elements {
    constructor(vararg elements: KtElement) : this(elements.toList())
    constructor(file: KtFile, rangeMarker: RangeMarker?) :
            this(rangeMarker?.let { marker ->
                file.elementsInRange(TextRange(marker.startOffset, marker.endOffset))
            } ?: listOf(file))
}
