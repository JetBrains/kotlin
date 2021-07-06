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


typealias ObjCFunctionApproximation = Int

data class PropertyApproximationKey(
    val name: CirName,
    val extensionReceiverParameterType: CirTypeSignature?
) {
    companion object {
        internal fun create(context: CirMemberContext, property: CirProperty): PropertyApproximationKey {
            return PropertyApproximationKey(
                name = property.name,
                extensionReceiverParameterType = property.extensionReceiver
                    ?.let { buildApproximationSignature(SignatureBuildingContext.create(context, property), it.type) }
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
        internal fun create(context: CirMemberContext, function: CirFunction): FunctionApproximationKey {
            return FunctionApproximationKey(
                name = function.name,
                valueParametersTypes = valueParameterTypes(context, function),
                extensionReceiverParameterType = function.extensionReceiver
                    ?.let { buildApproximationSignature(SignatureBuildingContext.create(context, function), it.type) },
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

data class ConstructorApproximationKey(
    val valueParametersTypes: Array<CirTypeSignature>,
    private val objCFunctionApproximation: ObjCFunctionApproximation
) {

    companion object {
        internal fun create(context: CirMemberContext, constructor: CirClassConstructor): ConstructorApproximationKey {
            return ConstructorApproximationKey(
                valueParametersTypes = valueParameterTypes(context, constructor),
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

private fun <T> valueParameterTypes(context: CirMemberContext, callable: T): Array<CirTypeSignature>
        where T : CirHasTypeParameters, T : CirCallableMemberWithParameters, T : CirMaybeCallableMemberOfClass {
    if (callable.valueParameters.isEmpty()) return emptyArray()
    return Array(callable.valueParameters.size) { index ->
        val parameter = callable.valueParameters[index]
        buildApproximationSignature(SignatureBuildingContext.create(context, callable), parameter.returnType)
    }
}

private val typeSignatureInterner = Interner<CirTypeSignature>()

internal fun buildApproximationSignature(context: SignatureBuildingContext, type: CirType): CirTypeSignature {
    return typeSignatureInterner.intern(StringBuilder().apply { appendTypeApproximationSignature(context, type) }.toString())
}

internal fun StringBuilder.appendTypeApproximationSignature(context: SignatureBuildingContext, type: CirType) {
    return when (type) {
        is CirFlexibleType -> appendFlexibleTypeApproximationSignature(context, type)
        is CirTypeParameterType -> appendTypeParameterTypeApproximationSignature(context.forTypeParameterTypes(), type)
        is CirClassOrTypeAliasType -> appendClassOrTypeAliasTypeApproximationSignature(context, type)
    }
}

internal fun StringBuilder.appendClassOrTypeAliasTypeApproximationSignature(
    context: SignatureBuildingContext, type: CirClassOrTypeAliasType
) {
    append(type.classifierId.toString())
    if (type.arguments.isNotEmpty()) {
        append("<")
        type.arguments.forEachIndexed { index, argument ->
            when (argument) {
                is CirRegularTypeProjection -> {
                    when (argument.projectionKind) {
                        Variance.INVARIANT -> Unit
                        Variance.IN_VARIANCE -> append("in ")
                        Variance.OUT_VARIANCE -> append("out ")
                    }
                    appendTypeApproximationSignature(context, argument.type)
                }
                is CirStarTypeProjection -> append("*")
            }
            if (index != type.arguments.lastIndex) {
                append(", ")
            }
        }
        append(">")
    }
    if (type.isMarkedNullable) append("?")
}

private fun StringBuilder.appendTypeParameterTypeApproximationSignature(
    context: TypeParameterTypeSignatureBuildingContext, type: CirTypeParameterType
) {
    val typeParameter = context.resolveTypeParameter(type.index)
    append(typeParameter.name)
    if (context.isVisitedFirstTime(type.index)) {
        append(" : [")
        typeParameter.upperBounds.forEachIndexed { index, upperBound ->
            appendTypeApproximationSignature(context, upperBound)
            if (index != typeParameter.upperBounds.lastIndex) append(", ")
        }
    }
    append("]")
    if (type.isMarkedNullable) append("?")
}

private fun StringBuilder.appendFlexibleTypeApproximationSignature(
    context: SignatureBuildingContext, type: CirFlexibleType
) {
    appendTypeApproximationSignature(context, type.lowerBound)
    append("..")
    appendTypeApproximationSignature(context, type.upperBound)
}

internal sealed interface SignatureBuildingContext {
    companion object {
        fun create(memberContext: CirMemberContext, functionOrPropertyOrConstructor: CirHasTypeParameters): SignatureBuildingContext {
            return DefaultSignatureBuildingContext(memberContext, functionOrPropertyOrConstructor)
        }
    }
}

private fun SignatureBuildingContext.forTypeParameterTypes(): TypeParameterTypeSignatureBuildingContext = when (this) {
    is DefaultSignatureBuildingContext -> TypeParameterTypeSignatureBuildingContext(memberContext, functionOrPropertyOrConstructor)
    is TypeParameterTypeSignatureBuildingContext -> this
}

private class DefaultSignatureBuildingContext(
    val memberContext: CirMemberContext,
    val functionOrPropertyOrConstructor: CirHasTypeParameters
) : SignatureBuildingContext

private class TypeParameterTypeSignatureBuildingContext(
    private val memberContext: CirMemberContext,
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
