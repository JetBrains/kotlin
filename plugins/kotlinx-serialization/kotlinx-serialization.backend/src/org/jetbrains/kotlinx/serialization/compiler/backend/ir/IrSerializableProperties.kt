/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.deserialization.registeredInSerializationPluginMetadataExtension
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyProperty
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationDescriptorSerializerPlugin
import org.jetbrains.kotlinx.serialization.compiler.resolve.*

class IrSerializableProperty(
    val ir: IrProperty,
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
 * This function checks if a deserialized property declares default value and has backing field.
 *
 * Returns (declaresDefaultValue, hasBackingField) boolean pair. Returns (false, false) for properties from current module.
 */
@OptIn(ObsoleteDescriptorBasedAPI::class, DirectDeclarationsAccess::class)
fun IrProperty.analyzeIfFromAnotherModule(): Pair<Boolean, Boolean> {
    return if (descriptor is DeserializedPropertyDescriptor) {
        // IrLazyProperty does not deserialize backing fields correctly, so we should fall back to info from descriptor.
        // DeserializedPropertyDescriptor can be encountered only after K1, so it is safe to check it.
        val hasDefault = descriptor.declaresDefaultValue()
        hasDefault to (descriptor.backingField != null || hasDefault)
    } else if (this is Fir2IrLazyProperty) {
        // Deserialized properties don't contain information about backing field, so we should extract this information from the
        // attribute, which is set if the property was mentioned in SerializationPluginMetadataExtensions.
        // Also, deserialized properties do not store default value (initializer expression) for property,
        // so we either should find corresponding constructor parameter and check its default, or rely on less strict check for default getter.
        // Comments are copied from PropertyDescriptor.declaresDefaultValue() as it has similar logic.
        val hasBackingField = fir.symbol.registeredInSerializationPluginMetadataExtension
        val matchingPrimaryConstructorParam = containingClass?.declarations?.filterIsInstance<FirPrimaryConstructor>()
            ?.singleOrNull()?.valueParameters?.find { it.name == this.name }
        if (matchingPrimaryConstructorParam != null) {
            // If property is a constructor parameter, check parameter default value
            // (serializable classes always have parameters-as-properties, so no name clash here)
            (matchingPrimaryConstructorParam.defaultValue != null) to hasBackingField
        } else {
            // If it is a body property, then it is likely to have initializer when getter is not specified
            // note this approach is not working well if we have smth like `get() = field`, but such cases on cross-module boundaries
            // should be very marginal. If we want to solve them, we need to add protobuf metadata extension.
            (fir.getter is FirDefaultPropertyGetter) to hasBackingField
        }
    } else {
        false to false
    }
}

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
        if (irClass.shouldHaveGeneratedMethods()) !it.annotations.hasAnnotation(SerializationAnnotations.serialTransientFqName)
        else !DescriptorVisibilities.isPrivate(it.visibility) && ((it.isVar && !it.annotations.hasAnnotation(SerializationAnnotations.serialTransientFqName)) || primaryParamsAsProps.contains(
            it
        )) && it.getter?.returnType != null // For some reason, some properties from Java (like java.net.URL.hostAddress) do not have getter. Let's ignore them, as they never have worked properly in K1 either.

    val (primaryCtorSerializableProps, bodySerializableProps) = properties
        .asSequence()
        .filter { !it.isFakeOverride && !it.isDelegated && it.origin != IrDeclarationOrigin.DELEGATED_MEMBER }
        .filter(::isPropSerializable)
        .map {
            val isConstructorParameterWithDefault = primaryParamsAsProps[it] ?: false
            val (isPropertyFromAnotherModuleDeclaresDefaultValue, isPropertyWithBackingFieldFromAnotherModule) = it.analyzeIfFromAnotherModule()
            val hasBackingField = when (it.origin) {
                IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB -> isPropertyWithBackingFieldFromAnotherModule
                else -> it.backingField != null
            }
            IrSerializableProperty(
                it,
                hasBackingField,
                it.backingField?.initializer.let { init -> init != null && !init.expression.isInitializePropertyFromParameter() } || isConstructorParameterWithDefault
                        || isPropertyFromAnotherModuleDeclaresDefaultValue,
                typeReplacement?.get(it) ?: it.getter!!.returnType as IrSimpleType
            )
        }
        .filterNot { it.transient }
        .partition { primaryParamsAsProps.contains(it.ir) }

    var serializableProps = run {
        val supers = irClass.getSuperClassNotAny()
        if (supers == null || !supers.shouldHaveGeneratedMethods()) {
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
