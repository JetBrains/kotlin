/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.diagnostic

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.js.descriptorUtils.getKotlinTypeFqName
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPureClassOrObject
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.*
import org.jetbrains.kotlinx.serialization.compiler.resolve.*
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationPackages.internalPackageFqName
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializersClassIds.enumSerializerId
import kotlin.collections.get

fun KotlinType.isGeneratedSerializableObject() =
    toClassDescriptor?.run { kind == ClassKind.OBJECT && hasSerializableOrMetaAnnotationWithoutArgs } == true

fun KotlinType.serialName(): String {
    val serializableDescriptor = this.toClassDescriptor!!
    return serializableDescriptor.serialName()
}

fun ClassDescriptor.serialName(): String {
    return annotations.serialNameValue ?: fqNameUnsafe.asString()
}

/**
 * Returns class descriptor for ContextSerializer or PolymorphicSerializer
 * if [annotations] contains @Contextual or @Polymorphic annotation
 */
fun analyzeSpecialSerializers(
    moduleDescriptor: ModuleDescriptor,
    annotations: Annotations
): ClassDescriptor? = when {
    annotations.hasAnnotation(SerializationAnnotations.contextualFqName) || annotations.hasAnnotation(SerializationAnnotations.contextualOnPropertyFqName) ->
        moduleDescriptor.getClassFromSerializationPackage(SpecialBuiltins.contextSerializer)
    // can be annotation on type usage, e.g. List<@Polymorphic Any>
    annotations.hasAnnotation(SerializationAnnotations.polymorphicFqName) ->
        moduleDescriptor.getClassFromSerializationPackage(SpecialBuiltins.polymorphicSerializer)
    else -> null
}

fun SerializationContextInFile.findTypeSerializerOrContextUnchecked(
    module: ModuleDescriptor,
    kType: KotlinType
): ClassDescriptor? {
    val annotations = kType.annotations
    if (kType.isTypeParameter()) return null
    annotations.serializableWith(module)?.let { return it.toClassDescriptor }
    this.additionalSerializersInScopeOfCurrentFile[kType.toClassDescriptor to kType.isMarkedNullable]?.let { return it }
    if (kType.isMarkedNullable) return findTypeSerializerOrContextUnchecked(module, kType.makeNotNullable())
    if (this.contextualKClassListInCurrentFile.contains(kType) == true) return module.getClassFromSerializationPackage(SpecialBuiltins.contextSerializer)
    return analyzeSpecialSerializers(module, annotations) ?: findTypeSerializer(module, kType)
}


fun findTypeSerializer(module: ModuleDescriptor, kType: KotlinType): ClassDescriptor? {
    val userOverride = kType.overriddenSerializer(module)
    if (userOverride != null) return userOverride.toClassDescriptor
    if (kType.isTypeParameter()) return null
    if (KotlinBuiltIns.isArray(kType)) return module.getClassFromInternalSerializationPackage(SpecialBuiltins.referenceArraySerializer)
    if (kType.isGeneratedSerializableObject()) return module.getClassFromInternalSerializationPackage(SpecialBuiltins.objectSerializer)
    val stdSer = findStandardKotlinTypeSerializer(module, kType) // see if there is a standard serializer
        ?: findEnumTypeSerializer(module, kType)
    if (stdSer != null) return stdSer
    if (kType.isInterface() && kType.toClassDescriptor?.isSealedSerializableInterface == false) return module.getClassFromSerializationPackage(
        SpecialBuiltins.polymorphicSerializer
    )
    return kType.toClassDescriptor?.classSerializer // check for serializer defined on the type
}

fun findStandardKotlinTypeSerializer(module: ModuleDescriptor, kType: KotlinType): ClassDescriptor? {
    val typeName = kType.getKotlinTypeFqName(false)
    val name = when (typeName) {
        "Z" -> if (kType.isBoolean()) PrimitiveBuiltins.booleanSerializer else null
        "B" -> if (kType.isByte()) PrimitiveBuiltins.byteSerializer else null
        "S" -> if (kType.isShort()) PrimitiveBuiltins.shortSerializer else null
        "I" -> if (kType.isInt()) PrimitiveBuiltins.intSerializer else null
        "J" -> if (kType.isLong()) PrimitiveBuiltins.longSerializer else null
        "F" -> if (kType.isFloat()) PrimitiveBuiltins.floatSerializer else null
        "D" -> if (kType.isDouble()) PrimitiveBuiltins.doubleSerializer else null
        "C" -> if (kType.isChar()) PrimitiveBuiltins.charSerializer else null
        else -> findStandardKotlinTypeSerializerName(typeName)
    } ?: return null
    val identifier = Name.identifier(name)
    return module.findClassAcrossModuleDependencies(ClassId(internalPackageFqName, identifier))
        ?: module.findClassAcrossModuleDependencies(ClassId(SerializationPackages.packageFqName, identifier))
}

fun findEnumTypeSerializer(module: ModuleDescriptor, kType: KotlinType): ClassDescriptor? {
    val classDescriptor = kType.toClassDescriptor ?: return null
    return if (classDescriptor.kind == ClassKind.ENUM_CLASS && !classDescriptor.isEnumWithLegacyGeneratedSerializer())
        module.findClassAcrossModuleDependencies(enumSerializerId)
    else null
}

fun KtPureClassOrObject.bodyPropertiesDescriptorsMap(
    bindingContext: BindingContext,
    filterUninitialized: Boolean = true
): Map<PropertyDescriptor, KtProperty> = declarations
    .asSequence()
    .filterIsInstance<KtProperty>()
    // can filter here because it's impossible to create body property w/ backing field w/o explicit delegating or initializing
    .filter { if (filterUninitialized) it.delegateExpressionOrInitializer != null else true }
    .associateBy { (bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, it] as? PropertyDescriptor)!! }

fun KtPureClassOrObject.primaryConstructorPropertiesDescriptorsMap(bindingContext: BindingContext): Map<PropertyDescriptor, KtParameter> =
    primaryConstructorParameters
        .asSequence()
        .filter { it.hasValOrVar() }
        .associateBy { bindingContext[BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, it]!! }
