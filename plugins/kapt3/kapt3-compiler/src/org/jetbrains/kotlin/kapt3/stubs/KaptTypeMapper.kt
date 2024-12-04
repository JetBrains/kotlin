/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.stubs

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.codegen.signature.AsmTypeFactory
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.load.kotlin.TypeMappingConfiguration
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.load.kotlin.internalName
import org.jetbrains.kotlin.load.kotlin.mapType
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.types.CommonSupertypes
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Type

internal object KaptTypeMapper {
    private val configuration = object : TypeMappingConfiguration<Type> {
        override fun commonSupertype(types: Collection<KotlinType>): KotlinType =
            CommonSupertypes.commonSupertype(types)

        override fun getPredefinedTypeForClass(classDescriptor: ClassDescriptor): Type? = null

        override fun getPredefinedInternalNameForClass(classDescriptor: ClassDescriptor): String? = null

        override fun processErrorType(kotlinType: KotlinType, descriptor: ClassDescriptor) {
        }

        override fun preprocessType(kotlinType: KotlinType): KotlinType? = null
    }

    fun mapType(type: KotlinType, mode: TypeMappingMode = TypeMappingMode.DEFAULT): Type =
        mapType(type, AsmTypeFactory, mode, configuration, null)

    fun mapKClassValue(kClassValue: KClassValue): Type {
        val value = kClassValue.value
        require(value is KClassValue.Value.NormalClass) { "Local classes are not supported here: $value" }

        val (classId, arrayDimensions) = value.value
        check(arrayDimensions <= 1) { "Arrays with >1 dimensions are not possible in annotations: $value" }
        if (classId.packageFqName == StandardNames.BUILT_INS_PACKAGE_FQ_NAME && arrayDimensions == 0) {
            val primitiveType = PrimitiveType.getByShortName(classId.relativeClassName.asString())
            if (primitiveType != null) {
                return Type.getType(JvmPrimitiveType.get(primitiveType).desc)
            }
            val primitiveArrayType = PrimitiveType.getByShortArrayName(classId.relativeClassName.asString())
            if (primitiveArrayType != null) {
                return Type.getType("[" + JvmPrimitiveType.get(primitiveArrayType).desc)
            }
        }

        val mapped = JavaToKotlinClassMap.mapKotlinToJava(classId.asSingleFqName().toUnsafe()) ?: classId
        return Type.getType("[".repeat(arrayDimensions) + "L" + mapped.internalName + ";")
    }
}
