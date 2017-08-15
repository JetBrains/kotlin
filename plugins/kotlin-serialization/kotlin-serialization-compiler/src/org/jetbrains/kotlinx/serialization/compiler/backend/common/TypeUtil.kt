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
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.containsTypeProjectionsInTopLevelArguments
import org.jetbrains.kotlin.types.typeUtil.isBoolean
import org.jetbrains.kotlin.types.typeUtil.isPrimitiveNumberType
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.enumSerializerId
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.polymorphicSerializerId
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.referenceArraySerializerId
import org.jetbrains.kotlinx.serialization.compiler.resolve.*

open class SerialTypeInfo(
        val property: SerializableProperty,
        val elementMethodPrefix: String,
        val serializer: ClassDescriptor? = null,
        val unit: Boolean = false
)

fun getSerialTypeInfo(property: SerializableProperty): SerialTypeInfo {
    val T = property.type
    return when {
        T.isPrimitiveNumberType() or T.isBoolean() -> SerialTypeInfo(property,
                                                                     T.nameIfStandardType.toString().capitalize())
        KotlinBuiltIns.isString(T) -> SerialTypeInfo(property, "String")
        KotlinBuiltIns.isUnit(T) -> SerialTypeInfo(property, "Unit", unit = true)
        KotlinBuiltIns.isPrimitiveArray(T) -> TODO("primitive arrays are not supported yet")
        KotlinBuiltIns.isNonPrimitiveArray(T.toClassDescriptor!!) -> {
            val serializer = property.serializer?.toClassDescriptor ?:
                             property.module.findClassAcrossModuleDependencies(referenceArraySerializerId)
            SerialTypeInfo(property, if (property.type.isMarkedNullable) "Nullable" else "", serializer)
        }
        T.toClassDescriptor?.kind == ClassKind.ENUM_CLASS -> {
            val serializer = property.serializer?.toClassDescriptor ?:
                             property.module.findClassAcrossModuleDependencies(enumSerializerId)
            SerialTypeInfo(property, if (property.type.isMarkedNullable) "Nullable" else "", serializer)
        }
        else -> {
            val serializer = findTypeSerializer(property.module, property.type)
            SerialTypeInfo(property, if (property.type.isMarkedNullable) "Nullable" else "", serializer)
        }
    }
}

fun findTypeSerializer(module: ModuleDescriptor, kType: KotlinType): ClassDescriptor? {
    return if (kType.requiresPolymorphism()) findPolymorphicSerializer(module)
    else kType.typeSerializer.toClassDescriptor // check for serializer defined on the type
         ?: findStandardKotlinTypeSerializer(module, kType) // otherwise see if there is a standard serializer
}

fun findStandardKotlinTypeSerializer(module: ModuleDescriptor, kType: KotlinType): ClassDescriptor? {
    val name = when(kType.getJetTypeFqName(false)) {
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
        "kotlin.collections.Collection", "kotlin.collections.List", "kotlin.collections.ArrayList" -> "ArrayListSerializer"
        "kotlin.collections.Set", "kotlin.collections.LinkedHashSet" -> "LinkedHashSetSerializer"
        "kotlin.collections.HashSet" -> "HashSetSerializer"
        "kotlin.collections.Map", "kotlin.collections.LinkedHashMap" -> "LinkedHashMapSerializer"
        "kotlin.collections.HashMap" -> "HashMapSerializer"
        "kotlin.collections.Map.Entry" -> "MapEntrySerializer"
        else -> return null
    }
    return module.findClassAcrossModuleDependencies(ClassId(internalPackageFqName, Name.identifier(name)))
}

fun KotlinType.requiresPolymorphism(): Boolean {
    return this.toClassDescriptor?.getSuperClassNotAny()?.isInternalSerializable == true
           || this.toClassDescriptor?.modality == Modality.OPEN
           || this.containsTypeProjectionsInTopLevelArguments() // List<*>
}

fun findPolymorphicSerializer(module: ModuleDescriptor): ClassDescriptor {
    return requireNotNull(module.findClassAcrossModuleDependencies(polymorphicSerializerId)) { "Can't locate polymorphic serializer definition" }
}