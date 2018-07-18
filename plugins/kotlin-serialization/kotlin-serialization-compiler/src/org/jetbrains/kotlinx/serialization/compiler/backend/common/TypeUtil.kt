/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.common

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.js.descriptorUtils.nameIfStandardType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.containsTypeProjectionsInTopLevelArguments
import org.jetbrains.kotlin.types.typeUtil.isBoolean
import org.jetbrains.kotlin.types.typeUtil.isPrimitiveNumberType
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.contextSerializerId
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.enumSerializerId
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.polymorphicSerializerId
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.referenceArraySerializerId
import org.jetbrains.kotlinx.serialization.compiler.resolve.*
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationPackages.internalPackageFqName

open class SerialTypeInfo(
    val property: SerializableProperty,
    val elementMethodPrefix: String,
    val serializer: ClassDescriptor? = null,
    val unit: Boolean = false
)

fun getSerialTypeInfo(property: SerializableProperty): SerialTypeInfo {
    val T = property.type
    val serializableWith = property.serializableWith?.toClassDescriptor
    if (serializableWith != null) return SerialTypeInfo(property, if (property.type.isMarkedNullable) "Nullable" else "", serializableWith)
    return when {
        T.isTypeParameter() -> SerialTypeInfo(property, if (property.type.isMarkedNullable) "Nullable" else "", null)
        T.isPrimitiveNumberType() or T.isBoolean() -> SerialTypeInfo(
            property,
            T.getJetTypeFqName(false).removePrefix("kotlin.") // i don't feel so good about it...
//          alternative:  KotlinBuiltIns.getPrimitiveType(T)!!.typeName.identifier
        )
        KotlinBuiltIns.isString(T) -> SerialTypeInfo(property, "String")
        KotlinBuiltIns.isUnit(T) -> SerialTypeInfo(property, "Unit", unit = true)
        KotlinBuiltIns.isPrimitiveArray(T) -> TODO("primitive arrays are not supported yet")
        KotlinBuiltIns.isNonPrimitiveArray(T.toClassDescriptor!!) -> {
            val serializer = property.serializableWith?.toClassDescriptor ?: property.module.findClassAcrossModuleDependencies(
                referenceArraySerializerId
            )
            SerialTypeInfo(property, if (property.type.isMarkedNullable) "Nullable" else "", serializer)
        }
        T.toClassDescriptor?.kind == ClassKind.ENUM_CLASS -> {
            val serializer = property.module.findClassAcrossModuleDependencies(enumSerializerId)
            SerialTypeInfo(property, if (property.type.isMarkedNullable) "Nullable" else "", serializer)
        }
        else -> {
            val serializer = findTypeSerializerOrContext(property.module, property.type)
            SerialTypeInfo(property, if (property.type.isMarkedNullable) "Nullable" else "", serializer)
        }
    }
}

fun findTypeSerializerOrContext(module: ModuleDescriptor, kType: KotlinType): ClassDescriptor? {
    return findTypeSerializer(module, kType)
            ?: module.findClassAcrossModuleDependencies(contextSerializerId)
}

fun findTypeSerializer(module: ModuleDescriptor, kType: KotlinType): ClassDescriptor? {
    val userOverride = kType.overridenSerializer
    if (userOverride != null) return userOverride.toClassDescriptor
    if (kType.requiresPolymorphism()) return findPolymorphicSerializer(module)
    if (kType.isTypeParameter()) return null
    if (KotlinBuiltIns.isArray(kType)) return module.getClassFromInternalSerializationPackage(SpecialBuiltins.referenceArraySerializer)
    return kType.typeSerializer.toClassDescriptor // check for serializer defined on the type
            ?: findStandardKotlinTypeSerializer(module, kType) // otherwise see if there is a standard serializer
            ?: findEnumTypeSerializer(module, kType)
}

fun findStandardKotlinTypeSerializer(module: ModuleDescriptor, kType: KotlinType): ClassDescriptor? {
    val name = when (kType.getJetTypeFqName(false)) {
        "kotlin.Unit" -> "UnitSerializer"
        "Z", "kotlin.Boolean" -> "BooleanSerializer"
        "B", "kotlin.Byte" -> "ByteSerializer"
        "S", "kotlin.Short" -> "ShortSerializer"
        "I", "kotlin.Int" -> "IntSerializer"
        "J", "kotlin.Long" -> "LongSerializer"
        "F", "kotlin.Float" -> "FloatSerializer"
        "D", "kotlin.Double" -> "DoubleSerializer"
        "C", "kotlin.Char" -> "CharSerializer"
        "kotlin.String" -> "StringSerializer"
        "kotlin.Pair" -> "PairSerializer"
        "kotlin.Triple" -> "TripleSerializer"
        "kotlin.collections.Collection", "kotlin.collections.List",
        "kotlin.collections.ArrayList", "kotlin.collections.MutableList" -> "ArrayListSerializer"
        "kotlin.collections.Set", "kotlin.collections.LinkedHashSet", "kotlin.collections.MutableSet" -> "LinkedHashSetSerializer"
        "kotlin.collections.HashSet" -> "HashSetSerializer"
        "kotlin.collections.Map", "kotlin.collections.LinkedHashMap", "kotlin.collections.MutableMap" -> "LinkedHashMapSerializer"
        "kotlin.collections.HashMap" -> "HashMapSerializer"
        "kotlin.collections.Map.Entry" -> "MapEntrySerializer"
        else -> return null
    }
    return module.findClassAcrossModuleDependencies(ClassId(internalPackageFqName, Name.identifier(name)))
}

fun findEnumTypeSerializer(module: ModuleDescriptor, kType: KotlinType): ClassDescriptor? {
    val classDescriptor = kType.toClassDescriptor ?: return null
    return if (classDescriptor.kind == ClassKind.ENUM_CLASS) module.findClassAcrossModuleDependencies(enumSerializerId) else null
}

fun KotlinType.requiresPolymorphism(): Boolean {
    return this.toClassDescriptor?.getSuperClassNotAny()?.isInternalSerializable == true
            || (this.toClassDescriptor?.modality != Modality.FINAL && this.toClassDescriptor?.unsubstitutedPrimaryConstructor != null) // open not java class
            || this.containsTypeProjectionsInTopLevelArguments() // List<*>
}

fun findPolymorphicSerializer(module: ModuleDescriptor): ClassDescriptor {
    return requireNotNull(module.findClassAcrossModuleDependencies(polymorphicSerializerId)) { "Can't locate polymorphic serializer definition" }
}

fun KtPureClassOrObject.bodyPropertiesDescriptorsMap(bindingContext: BindingContext): Map<PropertyDescriptor, KtProperty> = declarations
    .asSequence()
    .filterIsInstance<KtProperty>()
    // can filter here because it's impossible to create body property w/ backing field w/o explicit delegating or initializing
    .filter { it.delegateExpressionOrInitializer != null }
    .associateBy { (bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, it] as? PropertyDescriptor)!! }

fun KtPureClassOrObject.primaryPropertiesDescriptorsMap(bindingContext: BindingContext): Map<PropertyDescriptor, KtParameter> =
    primaryConstructorParameters
        .asSequence()
        .filter { it.hasValOrVar() }
        .associateBy { bindingContext[BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, it]!! }

fun KtPureClassOrObject.anonymousInitializers() = declarations
    .asSequence()
    .filterIsInstance<KtAnonymousInitializer>()
    .mapNotNull { it.body }
    .toList()
