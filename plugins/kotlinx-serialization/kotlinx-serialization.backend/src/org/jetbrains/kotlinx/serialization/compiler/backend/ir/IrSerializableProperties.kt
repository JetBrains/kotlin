/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationDescriptorSerializerPlugin
import org.jetbrains.kotlinx.serialization.compiler.resolve.*

class IrSerializableProperty(
    val ir: IrProperty,
    override val isConstructorParameterWithDefault: Boolean,
    hasBackingField: Boolean,
    declaresDefaultValue: Boolean,
    val type: IrSimpleType
) : ISerializableProperty {
    override val name = ir.annotations.serialNameValue ?: ir.name.asString()
    override val originalDescriptorName: Name = ir.name
    val genericIndex = type.genericIndex
    fun serializableWith(ctx: SerializationBaseContext) =
        ir.annotations.serializableWith() ?: analyzeSpecialSerializers(ctx, ir.annotations)

    override val optional = !ir.annotations.hasAnnotation(SerializationAnnotations.requiredAnnotationFqName) && declaresDefaultValue
    override val transient = ir.annotations.hasAnnotation(SerializationAnnotations.serialTransientFqName) || !hasBackingField
}

class IrSerializableProperties(
    override val serializableProperties: List<IrSerializableProperty>,
    override val isExternallySerializable: Boolean,
    override val serializableConstructorProperties: List<IrSerializableProperty>,
    override val serializableStandaloneProperties: List<IrSerializableProperty>
) : ISerializableProperties<IrSerializableProperty>

/**
 * typeReplacement should be populated from FakeOverrides and is used when we want to determine the type for property
 * accounting for generic substitutions performed in subclasses:
 *
 * ```
 *    @Serializable
 *    sealed class TypedSealedClass<T>(val a: T) {
 *        @Serializable
 *        data class Child(val y: Int) : TypedSealedClass<String>("10")
 *     }
 * ```
 * In this case, serializableProperties for TypedSealedClass is a listOf(IrSerProp(val a: T)),
 * but for Child is a listOf(IrSerProp(val a: String), IrSerProp(val y: Int)).
 *
 * Using this approach, we can correctly deserialize parent's properties in Child.Companion.deserialize()
 */
@OptIn(ObsoleteDescriptorBasedAPI::class)
internal fun serializablePropertiesForIrBackend(
    irClass: IrClass,
    serializationDescriptorSerializer: SerializationDescriptorSerializerPlugin? = null,
    typeReplacement: Map<IrProperty, IrSimpleType>? = null
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
        )) && it.getter?.returnType != null // For some reason, some properties from Java (like java.net.URL.hostAddress) do not have getter. Let's ignore them, as they never have worked properly in K1 either.

    val (primaryCtorSerializableProps, bodySerializableProps) = properties
        .asSequence()
        .filter { !it.isFakeOverride && !it.isDelegated }
        .filter(::isPropSerializable)
        .map {
            val isConstructorParameterWithDefault = primaryParamsAsProps[it] ?: false
            // FIXME: workaround because IrLazyProperty doesn't deserialize information about backing fields. Fallback to descriptor won't work with FIR.
            val isPropertyFromAnotherModuleDeclaresDefaultValue =
                it.descriptor is DeserializedPropertyDescriptor && it.descriptor.declaresDefaultValue()
            val isPropertyWithBackingFieldFromAnotherModule =
                it.descriptor is DeserializedPropertyDescriptor && (it.descriptor.backingField != null || isPropertyFromAnotherModuleDeclaresDefaultValue)
            IrSerializableProperty(
                it,
                isConstructorParameterWithDefault,
                it.backingField != null || isPropertyWithBackingFieldFromAnotherModule,
                it.backingField?.initializer.let { init -> init != null && !init.expression.isInitializePropertyFromParameter() } || isConstructorParameterWithDefault
                        || isPropertyFromAnotherModuleDeclaresDefaultValue,
                typeReplacement?.get(it) ?: it.getter!!.returnType as IrSimpleType
            )
        }
        .filterNot { it.transient }
        .partition { primaryParamsAsProps.contains(it.ir) }

    var serializableProps = run {
        val supers = irClass.getSuperClassNotAny()
        if (supers == null || !supers.isInternalSerializable) {
            primaryCtorSerializableProps + bodySerializableProps
        } else {
            val originalToTypeFromFO = typeReplacement ?: buildMap<IrProperty, IrSimpleType> {
                irClass.properties.filter { it.isFakeOverride }.forEach { prop ->
                    val orig = prop.resolveFakeOverride()
                    val type = prop.getter?.returnType as? IrSimpleType
                    if (orig != null && type != null) put(orig, type)
                }
            }
            serializablePropertiesForIrBackend(
                supers,
                serializationDescriptorSerializer,
                originalToTypeFromFO
            ).serializableProperties + primaryCtorSerializableProps + bodySerializableProps
        }
    }

    // FIXME: since descriptor from FIR does not have classProto in it(?), this line won't do anything
    serializableProps = restoreCorrectOrderFromClassProtoExtension(irClass.descriptor, serializableProps)

    val isExternallySerializable =
        irClass.isInternallySerializableEnum() || primaryConstructorParams.size == primaryParamsAsProps.size

    return IrSerializableProperties(serializableProps, isExternallySerializable, primaryCtorSerializableProps, bodySerializableProps)
}
