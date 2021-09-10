/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.commonizer.mergedtree

import gnu.trove.TIntHashSet
import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.utils.Interner
import org.jetbrains.kotlin.commonizer.utils.appendHashCode
import org.jetbrains.kotlin.commonizer.utils.hashCode
import org.jetbrains.kotlin.commonizer.utils.isObjCInteropCallableAnnotation
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
}

typealias ObjCFunctionApproximation = Int

data class PropertyApproximationKey(
    val name: CirName,
    val extensionReceiverParameterType: CirTypeSignature?
) {
    companion object {
        internal fun create(
            context: CirMemberContext,
            commonClassifierIdResolver: CirCommonClassifierIdResolver,
            property: CirProperty
        ): PropertyApproximationKey {
            return PropertyApproximationKey(
                name = property.name,
                extensionReceiverParameterType = property.extensionReceiver?.let {
                    buildApproximationSignature(SignatureBuildingContext.create(context, commonClassifierIdResolver, property), it.type)
                }
            )
        }
    }
}

data class FunctionApproximationKey(
    val name: CirName,
    val valueParametersTypes: Array<CirTypeSignature>,
    val extensionReceiverParameterType: CirTypeSignature?,
    val objCFunctionApproximation: ObjCFunctionApproximation
) {

    companion object {
        internal fun create(
            context: CirMemberContext,
            commonClassifierIdResolver: CirCommonClassifierIdResolver,
            function: CirFunction
        ): FunctionApproximationKey {
            return FunctionApproximationKey(
                name = function.name,
                valueParametersTypes = valueParameterTypes(context, commonClassifierIdResolver, function),
                extensionReceiverParameterType = function.extensionReceiver?.let {
                    buildApproximationSignature(SignatureBuildingContext.create(context, commonClassifierIdResolver, function), it.type)
                },
                objCFunctionApproximation = objCFunctionApproximation(function)
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is FunctionApproximationKey)
            return false

        return name == other.name
                && objCFunctionApproximation == other.objCFunctionApproximation
                && valueParametersTypes.contentEquals(other.valueParametersTypes)
                && extensionReceiverParameterType == other.extensionReceiverParameterType
    }

    override fun hashCode() = hashCode(name)
        .appendHashCode(valueParametersTypes)
        .appendHashCode(extensionReceiverParameterType)
        .appendHashCode(objCFunctionApproximation)
}

private val typeSignatureInterner = Interner<CirTypeSignature>()

data class ConstructorApproximationKey(
    val valueParametersTypes: Array<CirTypeSignature>,
    private val objCFunctionApproximation: ObjCFunctionApproximation
) {

    companion object {
        internal fun create(
            context: CirMemberContext, commonClassifierIdResolver: CirCommonClassifierIdResolver, constructor: CirClassConstructor
        ): ConstructorApproximationKey {
            return ConstructorApproximationKey(
                valueParametersTypes = valueParameterTypes(context, commonClassifierIdResolver, constructor),
                objCFunctionApproximation = objCFunctionApproximation(constructor)
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ConstructorApproximationKey)
            return false

        return objCFunctionApproximation == other.objCFunctionApproximation
                && valueParametersTypes.contentEquals(other.valueParametersTypes)
    }

    override fun hashCode() = hashCode(valueParametersTypes)
        .appendHashCode(objCFunctionApproximation)
}

private fun <T> objCFunctionApproximation(value: T): ObjCFunctionApproximation
        where T : CirHasAnnotations, T : CirCallableMemberWithParameters {
    return if (value.annotations.any { it.type.classifierId.isObjCInteropCallableAnnotation }) {
        value.valueParameters.fold(0) { acc, next -> acc.appendHashCode(next.name) }
    } else 0
}

private fun <T> valueParameterTypes(
    context: CirMemberContext,
    commonClassifierIdResolver: CirCommonClassifierIdResolver,
    callable: T
): Array<CirTypeSignature>
        where T : CirHasTypeParameters, T : CirCallableMemberWithParameters, T : CirMaybeCallableMemberOfClass {
    if (callable.valueParameters.isEmpty()) return emptyArray()
    return Array(callable.valueParameters.size) { index ->
        val parameter = callable.valueParameters[index]
        buildApproximationSignature(SignatureBuildingContext.create(context, commonClassifierIdResolver, callable), parameter.returnType)
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
    return typeSignatureInterner.intern(signature)
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
    add(context.commonClassifierIdResolver.findCommonId(type.classifierId) ?: type.classifierId)
    if (type.arguments.isNotEmpty()) {
        add(TypeSignatureElements.ArgumentsStartToken)
        type.arguments.forEachIndexed { index, argument ->
            when (argument) {
                is CirRegularTypeProjection -> {
                    when (argument.projectionKind) {
                        Variance.INVARIANT -> Unit
                        Variance.IN_VARIANCE -> add(TypeSignatureElements.InVariance)
                        Variance.OUT_VARIANCE -> add(TypeSignatureElements.OutVariance)
                    }
                    appendTypeApproximationSignature(context, argument.type)
                }
                is CirStarTypeProjection -> add(TypeSignatureElements.StarProjection)
            }
            if (index != type.arguments.lastIndex) {
                add(TypeSignatureElements.ArgumentsSeparator)
            }
        }
        add(TypeSignatureElements.ArgumentsEndToken)
    }
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

internal sealed interface SignatureBuildingContext {
    val commonClassifierIdResolver: CirCommonClassifierIdResolver

    companion object {
        fun create(
            memberContext: CirMemberContext,
            commonClassifierIdResolver: CirCommonClassifierIdResolver,
            functionOrPropertyOrConstructor: CirHasTypeParameters
        ): SignatureBuildingContext {
            return DefaultSignatureBuildingContext(memberContext, commonClassifierIdResolver, functionOrPropertyOrConstructor)
        }
    }
}

private fun SignatureBuildingContext.forTypeParameterTypes(): TypeParameterTypeSignatureBuildingContext = when (this) {
    is DefaultSignatureBuildingContext -> TypeParameterTypeSignatureBuildingContext(
        memberContext, commonClassifierIdResolver, functionOrPropertyOrConstructor
    )
    is TypeParameterTypeSignatureBuildingContext -> this
}

private class DefaultSignatureBuildingContext(
    val memberContext: CirMemberContext,
    override val commonClassifierIdResolver: CirCommonClassifierIdResolver,
    val functionOrPropertyOrConstructor: CirHasTypeParameters
) : SignatureBuildingContext

private class TypeParameterTypeSignatureBuildingContext(
    private val memberContext: CirMemberContext,
    override val commonClassifierIdResolver: CirCommonClassifierIdResolver,
    private val functionOrPropertyOrConstructor: CirHasTypeParameters
) : SignatureBuildingContext {

    private val alreadyVisitedParameterTypeIndices = TIntHashSet()

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
