/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.stubs

import org.jetbrains.kotlin.codegen.signature.AsmTypeFactory
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.load.kotlin.TypeMappingConfiguration
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.load.kotlin.mapType
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
}
