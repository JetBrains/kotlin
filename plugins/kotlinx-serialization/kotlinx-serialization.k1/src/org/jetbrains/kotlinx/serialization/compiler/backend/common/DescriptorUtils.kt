/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.common

import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlinx.serialization.compiler.resolve.*

object SerializationDescriptorUtils {
    fun getSyntheticLoadMember(serializerDescriptor: ClassDescriptor): FunctionDescriptor? = CodegenUtil.getMemberToGenerate(
        serializerDescriptor, SerialEntityNames.LOAD,
        serializerDescriptor::checkLoadMethodResult, serializerDescriptor::checkLoadMethodParameters
    )

    fun getSyntheticSaveMember(serializerDescriptor: ClassDescriptor): FunctionDescriptor? = CodegenUtil.getMemberToGenerate(
        serializerDescriptor, SerialEntityNames.SAVE,
        serializerDescriptor::checkSaveMethodResult, serializerDescriptor::checkSaveMethodParameters
    )
}
