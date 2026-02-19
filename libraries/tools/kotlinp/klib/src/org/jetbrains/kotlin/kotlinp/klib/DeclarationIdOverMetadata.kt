/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp.klib

import org.jetbrains.kotlin.kotlinp.klib.TypeArgumentId.VarianceId
import kotlin.metadata.*

private fun KmClassifier.classifierId(): ClassifierId = when (this) {
    is KmClassifier.Class -> ClassOrTypeAliasId(name)
    is KmClassifier.TypeAlias -> ClassOrTypeAliasId(name)
    is KmClassifier.TypeParameter -> TypeParameterId(id)
}

private fun KmVariance.varianceId(): VarianceId = when (this) {
    KmVariance.INVARIANT -> VarianceId.INVARIANT
    KmVariance.IN -> VarianceId.IN
    KmVariance.OUT -> VarianceId.OUT
}

private fun KmTypeProjection.typeArgumentId(): TypeArgumentId =
    if (this == KmTypeProjection.STAR) {
        TypeArgumentId.Star
    } else {
        val (variance, type) = this
        check(variance != null && type != null) { "Variance and type should not be null" }
        TypeArgumentId.Regular(type.typeId(), variance.varianceId())
    }

private fun KmType.typeId(): TypeId = TypeId(classifier.classifierId(), arguments.map { it.typeArgumentId() })

private fun KmValueParameter.valueParameterId(): ParameterId = ParameterId(name, type.typeId(), varargElementType != null)

internal fun KmClass.classId(): ClassOrTypeAliasId = ClassOrTypeAliasId(name)
internal fun KmClass.enumEntryId(enumEntry: KmEnumEntry): ClassOrTypeAliasId = ClassOrTypeAliasId("$name.${enumEntry.name}")
internal fun KmTypeAlias.typeAliasId(containerNamePrefix: String): ClassOrTypeAliasId = ClassOrTypeAliasId("$containerNamePrefix$name")

@OptIn(ExperimentalContextParameters::class)
fun KmFunction.functionId(containerNamePrefix: String): FunctionId = FunctionId(
    qualifiedName = "$containerNamePrefix$name",
    contextReceivers = contextParameters.map { it.type.typeId() },
    extensionReceiver = receiverParameterType?.typeId(),
    parameters = valueParameters.map { it.valueParameterId() },
    returnType = returnType.typeId()
)

@OptIn(ExperimentalContextParameters::class)
fun KmProperty.propertyId(containerNamePrefix: String): PropertyId = PropertyId(
    qualifiedName = "$containerNamePrefix$name",
    contextReceivers = contextParameters.map { it.type.typeId() },
    extensionReceiver = receiverParameterType?.typeId(),
    returnType = returnType.typeId()
)

internal fun KmProperty.getterId(propertyId: PropertyId): FunctionId = FunctionId(
    qualifiedName = "${propertyId.qualifiedName.dropLast(name.length)}<get-$name>",
    contextReceivers = propertyId.contextReceivers,
    extensionReceiver = propertyId.extensionReceiver,
    parameters = emptyList(),
    returnType = propertyId.returnType
)

internal fun KmProperty.setterId(propertyId: PropertyId, setterParameter: KmValueParameter?): FunctionId = FunctionId(
    qualifiedName = "${propertyId.qualifiedName.dropLast(name.length)}<set-$name>",
    contextReceivers = propertyId.contextReceivers,
    extensionReceiver = propertyId.extensionReceiver,
    parameters = listOf(ParameterId((setterParameter?.type ?: returnType).typeId(), false)),
    returnType = TypeId.UNIT
)

fun KmConstructor.constructorId(containerNamePrefix: String): ConstructorId = ConstructorId(
    qualifiedName = "$containerNamePrefix<init>",
    parameters = valueParameters.map { it.valueParameterId() }
)
