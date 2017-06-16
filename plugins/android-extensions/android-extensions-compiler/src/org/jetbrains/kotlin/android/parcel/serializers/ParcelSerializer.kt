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

package org.jetbrains.kotlin.android.parcel.serializers

import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import java.util.*
import java.util.concurrent.ConcurrentHashMap

interface ParcelSerializer {
    val asmType: Type

    fun writeValue(v: InstructionAdapter)
    fun readValue(v: InstructionAdapter)

    companion object {
        fun get(type: KotlinType, asmType: Type, typeMapper: KotlinTypeMapper, forceBoxed: Boolean = false): ParcelSerializer {
            val className = asmType.className

            return when {
                asmType.sort == Type.ARRAY -> {
                    val elementType = type.builtIns.getArrayElementType(type)

                    wrapToNullAwareIfNeeded(
                            type,
                            ArrayParcelSerializer(asmType, get(elementType, typeMapper.mapType(elementType), typeMapper)))
                }

                asmType.isPrimitive() -> {
                    if (forceBoxed || type.isMarkedNullable)
                        wrapToNullAwareIfNeeded(type, BoxedPrimitiveTypeParcelSerializer.forUnboxedType(asmType))
                    else
                        PrimitiveTypeParcelSerializer.getInstance(asmType)
                }

                asmType.isString() -> NullCompliantObjectParcelSerializer(asmType,
                        Method("writeString"),
                        Method("readString"))

                className == List::class.java.canonicalName
                    || className == ArrayList::class.java.canonicalName
                    || className == LinkedList::class.java.canonicalName
                    || className == Set::class.java.canonicalName
                    || className == HashSet::class.java.canonicalName
                    || className == LinkedHashSet::class.java.canonicalName
                    || className == TreeSet::class.java.canonicalName
                -> {
                    val elementType = type.arguments.single().type
                    val elementSerializer = get(elementType, typeMapper.mapType(elementType), typeMapper, forceBoxed = true)
                    wrapToNullAwareIfNeeded(type, ListSetParcelSerializer(asmType, elementSerializer))
                }

                className == Map::class.java.canonicalName
                    || className == HashMap::class.java.canonicalName
                    || className == LinkedHashMap::class.java.canonicalName
                    || className == TreeMap::class.java.canonicalName
                    || className == ConcurrentHashMap::class.java.canonicalName
                -> {
                    val (keyType, valueType) = type.arguments.apply { assert(this.size == 2) }
                    val keySerializer = get(keyType.type, typeMapper.mapType(keyType.type), typeMapper, forceBoxed = true)
                    val valueSerializer = get(valueType.type, typeMapper.mapType(valueType.type), typeMapper, forceBoxed = true)
                    wrapToNullAwareIfNeeded(type, MapParcelSerializer(asmType, keySerializer, valueSerializer))
                }

                asmType.isBoxedPrimitive() -> wrapToNullAwareIfNeeded(type, BoxedPrimitiveTypeParcelSerializer.forBoxedType(asmType))

                asmType.isBlob() -> NullCompliantObjectParcelSerializer(asmType,
                        Method("writeBlob"),
                        Method("readBlob"))

                asmType.isSize() -> wrapToNullAwareIfNeeded(type, NullCompliantObjectParcelSerializer(asmType,
                        Method("writeSize"),
                        Method("readSize")))

                asmType.isSizeF() -> wrapToNullAwareIfNeeded(type, NullCompliantObjectParcelSerializer(asmType,
                        Method("writeSizeF"),
                        Method("readSizeF")))

                asmType.isBundle() -> NullCompliantObjectParcelSerializer(asmType,
                        Method("writeBundle"),
                        Method("readBundle"))

                asmType.isPersistableBundle() -> NullCompliantObjectParcelSerializer(asmType,
                        Method("writeBundle"),
                        Method("readBundle"))

                asmType.isSparseBooleanArray() -> NullCompliantObjectParcelSerializer(asmType,
                        Method("writeSparseBooleanArray"),
                        Method("readSparseBooleanArray"))

                asmType.isSparseIntArray() -> wrapToNullAwareIfNeeded(type, SparseArrayParcelSerializer(
                        asmType, PrimitiveTypeParcelSerializer.getInstance(Type.INT_TYPE)))

                asmType.isSparseLongArray() -> wrapToNullAwareIfNeeded(type, SparseArrayParcelSerializer(
                        asmType, PrimitiveTypeParcelSerializer.getInstance(Type.LONG_TYPE)))

                asmType.isSparseArray() -> {
                    val elementType = type.arguments.single().type
                    val elementSerializer = get(elementType, typeMapper.mapType(elementType), typeMapper, forceBoxed = true)
                    wrapToNullAwareIfNeeded(type, SparseArrayParcelSerializer(asmType, elementSerializer))
                }

                type.isException() -> wrapToNullAwareIfNeeded(type, NullCompliantObjectParcelSerializer(asmType,
                        Method("writeException"),
                        Method("readException")))

                asmType.isFileDescriptor() -> wrapToNullAwareIfNeeded(type, NullCompliantObjectParcelSerializer(asmType,
                        Method("writeRawFileDescriptor"),
                        Method("readRawFileDescriptor")))

                type.isSerializable() -> NullCompliantObjectParcelSerializer(asmType,
                        Method("writeSerializable"),
                        Method("readSerializable"))

                else -> GenericParcelSerializer
            }
        }
        private fun wrapToNullAwareIfNeeded(type: KotlinType, serializer: ParcelSerializer) = when {
            type.isMarkedNullable -> NullAwareParcelSerializerWrapper(serializer)
            else -> serializer
        }

        private fun Type.isBlob() = this.sort == Type.ARRAY && this.elementType == Type.BYTE_TYPE
        private fun Type.isString() = this.descriptor == "Ljava/lang/String;"
        private fun Type.isSize() = this.descriptor == "Landroid/util/Size;"
        private fun Type.isSizeF() = this.descriptor == "Landroid/util/SizeF;"
        private fun Type.isFileDescriptor() = this.descriptor == "Ljava/io/FileDescriptor;"
        private fun Type.isBundle() = this.descriptor == "Landroid/os/Bundle;"
        private fun Type.isPersistableBundle() = this.descriptor == "Landroid/os/PersistableBundle;"
        private fun Type.isSparseBooleanArray() = this.descriptor == "Landroid/util/SparseBooleanArray;"
        private fun Type.isSparseIntArray() = this.descriptor == "Landroid/util/SparseIntArray;"
        private fun Type.isSparseLongArray() = this.descriptor == "Landroid/util/SparseLongArray;"
        private fun Type.isSparseArray() = this.descriptor == "Landroid/util/SparseArray;"
        private fun KotlinType.isSerializable() = matchesFqNameWithSupertypes("java.io.Serializable")
        private fun KotlinType.isException() = matchesFqNameWithSupertypes("java.lang.Exception")

        private fun Type.isPrimitive(): Boolean = when (this.sort) {
            Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT, Type.FLOAT, Type.LONG, Type.DOUBLE -> true
            else -> false
        }

        private fun Type.isBoxedPrimitive(): Boolean = when(this.descriptor) {
            "Ljava/lang/Boolean;",
            "Ljava/lang/Character;",
            "Ljava/lang/Byte;",
            "Ljava/lang/Short;",
            "Ljava/lang/Integer;",
            "Ljava/lang/Float;",
            "Ljava/lang/Long;",
            "Ljava/lang/Double;" -> true
            else -> false
        }

        private fun KotlinType.matchesFqNameWithSupertypes(fqName: String): Boolean {
            if (this.matchesFqName(fqName)) {
                return true
            }

            return this.constructor.supertypes.any { it.matchesFqName(fqName) }
        }

        private fun KotlinType.matchesFqName(fqName: String): Boolean {
            return this.constructor.declarationDescriptor?.fqNameSafe?.asString() == fqName
        }
    }
}