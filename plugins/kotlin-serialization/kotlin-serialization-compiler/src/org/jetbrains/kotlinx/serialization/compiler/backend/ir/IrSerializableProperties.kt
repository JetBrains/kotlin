/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.hasDefaultValue
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationDescriptorSerializerPlugin
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginContext
import org.jetbrains.kotlinx.serialization.compiler.resolve.*

class IrSerializableProperty(
    val ir: IrProperty,
    override val isConstructorParameterWithDefault: Boolean,
    hasBackingField: Boolean,
    declaresDefaultValue: Boolean
) : ISerializableProperty {
    override val name = ir.annotations.serialNameValue ?: ir.name.asString()
    override val originalDescriptorName: Name = ir.name
    val type = ir.getter!!.returnType as IrSimpleType
    val genericIndex = type.genericIndex
    fun serializableWith(ctx: SerializationPluginContext) = ir.annotations.serializableWith() ?: analyzeSpecialSerializers(ctx, ir.annotations)
    override val optional = !ir.annotations.hasAnnotation(SerializationAnnotations.requiredAnnotationFqName) && declaresDefaultValue
    override val transient = ir.annotations.hasAnnotation(SerializationAnnotations.serialTransientFqName) || !hasBackingField
}

class IrSerializableProperties(
    override val serializableProperties: List<IrSerializableProperty>,
    override val isExternallySerializable: Boolean,
    override val serializableConstructorProperties: List<IrSerializableProperty>,
    override val serializableStandaloneProperties: List<IrSerializableProperty>
) : ISerializableProperties<IrSerializableProperty>

internal fun serializablePropertiesForIrBackend(
    irClass: IrClass,
    serializationDescriptorSerializer: SerializationDescriptorSerializerPlugin? = null
): IrSerializableProperties {
    val properties = irClass.properties.toList()
    val primaryConstructorParams = irClass.primaryConstructor?.valueParameters.orEmpty()
    val primaryParamsAsProps = properties.associateBy { it.name }.let { namesMap ->
        primaryConstructorParams.mapNotNull {
            if (it.name !in namesMap) null else namesMap.getValue(it.name) to it.hasDefaultValue()
        }.toMap()
    }

    fun isPropSerializable(it: IrProperty) =
        if (irClass.isInternalSerializable) !it.annotations.hasAnnotation(SerializationAnnotations.serialTransientFqName)
        else !DescriptorVisibilities.isPrivate(it.visibility) && ((it.isVar && !it.annotations.hasAnnotation(SerializationAnnotations.serialTransientFqName)) || primaryParamsAsProps.contains(
            it
        ))

    val (primaryCtorSerializableProps, bodySerializableProps) = properties
        .asSequence()
        .filter { !it.isFakeOverride && !it.isDelegated }
        .filter(::isPropSerializable)
        .map {
            val isConstructorParameterWithDefault = primaryParamsAsProps[it] ?: false
            // FIXME: workaround because IrLazyProperty doesn't deserialize information about backing fields. Fallback to descriptor won't work with FIR.
            val isPropertyFromAnotherModuleDeclaresDefaultValue = it.descriptor is DeserializedPropertyDescriptor &&  it.descriptor.declaresDefaultValue()
            val isPropertyWithBackingFieldFromAnotherModule = it.descriptor is DeserializedPropertyDescriptor && (it.descriptor.backingField != null || isPropertyFromAnotherModuleDeclaresDefaultValue)
            IrSerializableProperty(
                it,
                isConstructorParameterWithDefault,
                it.backingField != null || isPropertyWithBackingFieldFromAnotherModule,
                it.backingField?.initializer.let { init -> init != null && !init.expression.isInitializePropertyFromParameter() } || isConstructorParameterWithDefault
                        || isPropertyFromAnotherModuleDeclaresDefaultValue
            )
        }
        .filterNot { it.transient }
        .partition { primaryParamsAsProps.contains(it.ir) }

    var serializableProps = run {
        val supers = irClass.getSuperClassNotAny()
        if (supers == null || !supers.isInternalSerializable)
            primaryCtorSerializableProps + bodySerializableProps
        else
            serializablePropertiesForIrBackend(
                supers,
                serializationDescriptorSerializer
            ).serializableProperties + primaryCtorSerializableProps + bodySerializableProps
    }

    // FIXME: since descriptor from FIR does not have classProto in it(?), this line won't do anything
    serializableProps = restoreCorrectOrderFromClassProtoExtension(irClass.descriptor, serializableProps)

    val isExternallySerializable =
        irClass.isInternallySerializableEnum() || primaryConstructorParams.size == primaryParamsAsProps.size

    return IrSerializableProperties(serializableProps, isExternallySerializable, primaryCtorSerializableProps, bodySerializableProps)
}