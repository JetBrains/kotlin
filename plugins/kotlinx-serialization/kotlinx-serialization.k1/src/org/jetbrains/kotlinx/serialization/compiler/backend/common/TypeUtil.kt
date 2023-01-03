/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.common

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.backend.common.CompilationException
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.js.descriptorUtils.getKotlinTypeFqName
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPureClassOrObject
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.*
import org.jetbrains.kotlinx.serialization.compiler.resolve.*
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializersClassIds.enumSerializerId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializersClassIds.referenceArraySerializerId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationPackages.internalPackageFqName

open class SerialTypeInfo(
    val property: SerializableProperty,
    val elementMethodPrefix: String,
    val serializer: ClassDescriptor? = null
)

fun AbstractSerialGenerator.findAddOnSerializer(propertyType: KotlinType, module: ModuleDescriptor): ClassDescriptor? {
    additionalSerializersInScopeOfCurrentFile[propertyType.toClassDescriptor to propertyType.isMarkedNullable]?.let { return it }
    if (propertyType in contextualKClassListInCurrentFile)
        return module.getClassFromSerializationPackage(SpecialBuiltins.contextSerializer)
    if (propertyType.toClassDescriptor?.annotations?.hasAnnotation(SerializationAnnotations.polymorphicFqName) == true)
        return module.getClassFromSerializationPackage(SpecialBuiltins.polymorphicSerializer)
    if (propertyType.isMarkedNullable) return findAddOnSerializer(propertyType.makeNotNullable(), module)
    return null
}

fun KotlinType.isGeneratedSerializableObject() =
    toClassDescriptor?.run { kind == ClassKind.OBJECT && hasSerializableOrMetaAnnotationWithoutArgs } == true

@Suppress("FunctionName", "LocalVariableName")
fun AbstractSerialGenerator.getSerialTypeInfo(property: SerializableProperty): SerialTypeInfo {
    fun SerializableInfo(serializer: ClassDescriptor?) =
        SerialTypeInfo(property, if (property.type.isMarkedNullable) "Nullable" else "", serializer)

    val T = property.type
    property.serializableWith?.toClassDescriptor?.let { return SerializableInfo(it) }
    findAddOnSerializer(T, property.module)?.let { return SerializableInfo(it) }
    T.overriddenSerializer(property.module)?.toClassDescriptor?.let { return SerializableInfo(it) }
    return when {
        T.isTypeParameter() -> SerialTypeInfo(property, if (property.type.isMarkedNullable) "Nullable" else "", null)
        T.isPrimitiveNumberType() or T.isBoolean() -> SerialTypeInfo(
            property,
            T.getKotlinTypeFqName(false).removePrefix("kotlin.") // i don't feel so good about it...
//          alternative:  KotlinBuiltIns.getPrimitiveType(T)!!.typeName.identifier
        )
        KotlinBuiltIns.isString(T) -> SerialTypeInfo(property, "String")
        KotlinBuiltIns.isNonPrimitiveArray(T.toClassDescriptor!!) -> {
            val serializer = property.serializableWith?.toClassDescriptor ?: property.module.findClassAcrossModuleDependencies(
                referenceArraySerializerId
            )
            SerializableInfo(serializer)
        }
        else -> {
            val serializer =
                findTypeSerializerOrContext(property.module, property.type, property.descriptor.findPsi())
            SerializableInfo(serializer)
        }
    }
}

fun AbstractSerialGenerator?.allSealedSerializableSubclassesFor(
    klass: ClassDescriptor,
    module: ModuleDescriptor
): Pair<List<KotlinType>, List<ClassDescriptor>> {
    assert(klass.modality == Modality.SEALED)
    fun recursiveSealed(klass: ClassDescriptor): Collection<ClassDescriptor> {
        return klass.sealedSubclasses.flatMap { if (it.modality == Modality.SEALED) recursiveSealed(it) else setOf(it) }
    }

    val serializableSubtypes = recursiveSealed(klass).map { it.toSimpleType() }
    return serializableSubtypes.mapNotNull { subtype ->
        findTypeSerializerOrContextUnchecked(module, subtype)?.let { Pair(subtype, it) }
    }.unzip()
}

fun KotlinType.serialName(): String {
    val serializableDescriptor = this.toClassDescriptor!!
    return serializableDescriptor.serialName()
}

fun ClassDescriptor.serialName(): String {
    return annotations.serialNameValue ?: fqNameUnsafe.asString()
}

val ClassDescriptor.isStaticSerializable: Boolean get() = this.declaredTypeParameters.isEmpty()

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

fun AbstractSerialGenerator?.findTypeSerializerOrContextUnchecked(
    module: ModuleDescriptor,
    kType: KotlinType
): ClassDescriptor? {
    val annotations = kType.annotations
    if (kType.isTypeParameter()) return null
    annotations.serializableWith(module)?.let { return it.toClassDescriptor }
    this?.additionalSerializersInScopeOfCurrentFile?.get(kType.toClassDescriptor to kType.isMarkedNullable)?.let { return it }
    if (kType.isMarkedNullable) return findTypeSerializerOrContextUnchecked(module, kType.makeNotNullable())
    if (this?.contextualKClassListInCurrentFile?.contains(kType) == true) return module.getClassFromSerializationPackage(SpecialBuiltins.contextSerializer)
    return analyzeSpecialSerializers(module, annotations) ?: findTypeSerializer(module, kType)
}

fun AbstractSerialGenerator?.findTypeSerializerOrContext(
    module: ModuleDescriptor,
    kType: KotlinType,
    sourceElement: PsiElement? = null
): ClassDescriptor? {
    if (kType.isTypeParameter()) return null
    return findTypeSerializerOrContextUnchecked(module, kType) ?: throw CompilationException(
        "Serializer for element of type $kType has not been found.\n" +
                "To use context serializer as fallback, explicitly annotate element with @Contextual",
        null,
        sourceElement
    )
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

fun KtPureClassOrObject.anonymousInitializers() = declarations
    .asSequence()
    .filterIsInstance<KtAnonymousInitializer>()
    .mapNotNull { it.body }
    .toList()
