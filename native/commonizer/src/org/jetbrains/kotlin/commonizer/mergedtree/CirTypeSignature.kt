/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.mergedtree

import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.utils.CommonizerIntSet
import org.jetbrains.kotlin.commonizer.utils.Interner
import org.jetbrains.kotlin.types.Variance

class CirTypeSignature {
    private val elements = ArrayList<Any>()

    private var hashCode = 0

    fun add(element: Any) {
        elements.add(element)
        hashCode = 31 * hashCode + element.hashCode()
    }

    override fun hashCode(): Int = hashCode

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is CirTypeSignature) return false
        if (other.hashCode != this.hashCode) return false
        if (other.elements != this.elements) return false
        return true
    }

    override fun toString(): String {
        return elements.joinToString("")
    }

    companion object {
        val interner = Interner<CirTypeSignature>()
    }
}

internal fun SignatureBuildingContext(
    memberContext: CirMemberContext, functionOrPropertyOrConstructor: CirHasTypeParameters,
    classifierSignatureBuildingContext: ClassifierSignatureBuildingContext = ClassifierSignatureBuildingContext.Default,
    argumentsSignatureBuildingContext: ArgumentsSignatureBuildingContext = ArgumentsSignatureBuildingContext.Default,
): SignatureBuildingContext {
    return DefaultSignatureBuildingContext(
        memberContext, classifierSignatureBuildingContext, argumentsSignatureBuildingContext, functionOrPropertyOrConstructor
    )
}

internal sealed interface SignatureBuildingContext {
    val memberContext: CirMemberContext
    val classifierSignatureBuildingContext: ClassifierSignatureBuildingContext
    val argumentsSignatureBuildingContext: ArgumentsSignatureBuildingContext
}

internal sealed interface ClassifierSignatureBuildingContext {
    fun appendSignature(signature: CirTypeSignature, classifierId: CirEntityId)

    object Default : ClassifierSignatureBuildingContext {
        override fun appendSignature(signature: CirTypeSignature, classifierId: CirEntityId) = signature.add(classifierId)
    }

    class TypeAliasInvariant(private val associatedIdsResolver: AssociatedClassifierIdsResolver) : ClassifierSignatureBuildingContext {
        override fun appendSignature(signature: CirTypeSignature, classifierId: CirEntityId) {
            return signature.add(associatedIdsResolver.resolveAssociatedIds(classifierId) ?: classifierId)
        }
    }
}

internal sealed interface ArgumentsSignatureBuildingContext {
    fun appendSignature(signature: CirTypeSignature, context: SignatureBuildingContext, arguments: List<CirTypeProjection>)

    object Default : ArgumentsSignatureBuildingContext {
        override fun appendSignature(signature: CirTypeSignature, context: SignatureBuildingContext, arguments: List<CirTypeProjection>) {
            if (arguments.isEmpty()) return
            signature.add(TypeSignatureElements.ArgumentsStartToken)
            arguments.forEachIndexed { index, argument ->
                when (argument) {
                    is CirRegularTypeProjection -> {
                        when (argument.projectionKind) {
                            Variance.INVARIANT -> Unit
                            Variance.IN_VARIANCE -> signature.add(TypeSignatureElements.InVariance)
                            Variance.OUT_VARIANCE -> signature.add(TypeSignatureElements.OutVariance)
                        }
                        signature.appendTypeApproximationSignature(context, argument.type)
                    }
                    is CirStarTypeProjection -> signature.add(TypeSignatureElements.StarProjection)
                }
                if (index != arguments.lastIndex) {
                    signature.add(TypeSignatureElements.ArgumentsSeparator)
                }
            }
            signature.add(TypeSignatureElements.ArgumentsEndToken)
        }
    }

    /**
     * Won't render any type arguments
     */
    object SkipArguments : ArgumentsSignatureBuildingContext {
        override fun appendSignature(
            signature: CirTypeSignature, context: SignatureBuildingContext, arguments: List<CirTypeProjection>
        ) = Unit
    }
}


private enum class TypeSignatureElements(val stringRepresentation: String) {
    ArgumentsStartToken("<"),
    ArgumentsEndToken(">"),
    ArgumentsSeparator(", "),
    NullableToken("?"),
    UpperBoundsStartToken(" : ["),
    UpperBoundsEndToken("]"),
    InVariance("in "),
    OutVariance("out "),
    StarProjection("*"),
    FlexibleTypeSeparator("..");

    override fun toString(): String {
        return stringRepresentation
    }
}

internal fun buildApproximationSignature(context: SignatureBuildingContext, type: CirType): CirTypeSignature {
    val signature = CirTypeSignature()
    signature.appendTypeApproximationSignature(context, type)
    return CirTypeSignature.interner.intern(signature)
}

internal fun CirTypeSignature.appendTypeApproximationSignature(context: SignatureBuildingContext, type: CirType) {
    return when (type) {
        is CirFlexibleType -> appendFlexibleTypeApproximationSignature(context, type)
        is CirTypeParameterType -> appendTypeParameterTypeApproximationSignature(context.forTypeParameterTypes(), type)
        is CirClassOrTypeAliasType -> appendClassOrTypeAliasTypeApproximationSignature(context, type)
    }
}

internal fun CirTypeSignature.appendClassOrTypeAliasTypeApproximationSignature(
    context: SignatureBuildingContext, type: CirClassOrTypeAliasType
) {
    context.classifierSignatureBuildingContext.appendSignature(this, type.classifierId)
    context.argumentsSignatureBuildingContext.appendSignature(this, context, type.arguments)
    if (type.isMarkedNullable) add(TypeSignatureElements.NullableToken)
}

private fun CirTypeSignature.appendTypeParameterTypeApproximationSignature(
    context: TypeParameterTypeSignatureBuildingContext, type: CirTypeParameterType
) {
    val typeParameter = context.resolveTypeParameter(type.index)
    add(typeParameter.name)
    if (context.isVisitedFirstTime(type.index)) {
        add(TypeSignatureElements.UpperBoundsStartToken)
        typeParameter.upperBounds.forEachIndexed { index, upperBound ->
            appendTypeApproximationSignature(context, upperBound)
            if (index != typeParameter.upperBounds.lastIndex) add(TypeSignatureElements.ArgumentsSeparator)
        }
        add(TypeSignatureElements.UpperBoundsEndToken)
    }
    if (type.isMarkedNullable) add(TypeSignatureElements.NullableToken)
}

private fun CirTypeSignature.appendFlexibleTypeApproximationSignature(
    context: SignatureBuildingContext, type: CirFlexibleType
) {
    appendTypeApproximationSignature(context, type.lowerBound)
    add(TypeSignatureElements.FlexibleTypeSeparator)
    appendTypeApproximationSignature(context, type.upperBound)
}

private fun SignatureBuildingContext.forTypeParameterTypes(): TypeParameterTypeSignatureBuildingContext = when (this) {
    is DefaultSignatureBuildingContext -> TypeParameterTypeSignatureBuildingContext(
        memberContext, classifierSignatureBuildingContext, argumentsSignatureBuildingContext, functionOrPropertyOrConstructor
    )
    is TypeParameterTypeSignatureBuildingContext -> this
}

private class DefaultSignatureBuildingContext(
    override val memberContext: CirMemberContext,
    override val classifierSignatureBuildingContext: ClassifierSignatureBuildingContext,
    override val argumentsSignatureBuildingContext: ArgumentsSignatureBuildingContext,
    val functionOrPropertyOrConstructor: CirHasTypeParameters
) : SignatureBuildingContext

private class TypeParameterTypeSignatureBuildingContext(
    override val memberContext: CirMemberContext,
    override val classifierSignatureBuildingContext: ClassifierSignatureBuildingContext,
    override val argumentsSignatureBuildingContext: ArgumentsSignatureBuildingContext,
    private val functionOrPropertyOrConstructor: CirHasTypeParameters
) : SignatureBuildingContext {

    private val alreadyVisitedParameterTypeIndices = CommonizerIntSet()

    fun isVisitedFirstTime(typeParameterIndex: Int): Boolean {
        return alreadyVisitedParameterTypeIndices.add(typeParameterIndex)
    }

    fun resolveTypeParameter(index: Int): CirTypeParameter {
        var indexOffset = 0
        memberContext.classes.forEach { clazz ->
            val indexInClass = index - indexOffset
            if (indexInClass >= 0 && indexInClass <= clazz.typeParameters.lastIndex) {
                return clazz.typeParameters[indexInClass]
            }
            indexOffset += clazz.typeParameters.size
        }

        return functionOrPropertyOrConstructor.typeParameters[index - indexOffset]
    }
}
