/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.nullabilityAnalysis

import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal class ContextCreator(private val getNullability: (KtTypeElement) -> Nullability) {

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
        val nullability = getNullability(this)
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


fun nullabilityByUndefinedNullabilityComment(typeElement: KtTypeElement): Nullability {
    val undefinedNullabilityComment =
        typeElement.parent?.safeAs<KtTypeReference>()?.getUndefinedNullabilityComment()?.also { it.delete() }
    return when {
        undefinedNullabilityComment != null -> Nullability.UNKNOWN
        typeElement is KtNullableType -> Nullability.NULLABLE
        else -> Nullability.NOT_NULL
    }
}


private fun KtElement.getUndefinedNullabilityComment(): PsiComment? =
    prevSibling?.safeAs<PsiComment>()?.takeIf { it.text == UNDEFINED_NULLABILITY_COMMENT }
        ?: parent?.safeAs<KtTypeProjection>()?.getUndefinedNullabilityComment()


fun prepareTypeElementByMakingAllTypesNullableConsideringNullabilityComment(typeElement: KtTypeElement) {
    val hasUndefinedNullabilityComment =
        typeElement.parent?.safeAs<KtTypeReference>()?.getUndefinedNullabilityComment() != null
    if (hasUndefinedNullabilityComment) {
        typeElement.changeNullability(toNullable = true)
    }
}

fun preapareTypeElementByMakingAllTypesNullable(typeElement: KtTypeElement) {
    typeElement.changeNullability(toNullable = true)
}


internal const val UNDEFINED_NULLABILITY_COMMENT = "/*UNDEFINED*/"


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
