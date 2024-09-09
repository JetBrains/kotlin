/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.jvm

import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.ENUMS_FILE
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.KSERIALIZER_CLASS
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

internal val enumFactoriesType = Type.getObjectType("kotlinx/serialization/internal/${ENUMS_FILE}Kt")

internal val kSerializerType = Type.getObjectType("kotlinx/serialization/$KSERIALIZER_CLASS")
internal val kSerializerArrayType = Type.getObjectType("[Lkotlinx/serialization/$KSERIALIZER_CLASS;")

internal val annotationType = Type.getObjectType("java/lang/annotation/Annotation")
internal val annotationArrayType = Type.getObjectType("[${annotationType.descriptor}")
internal val doubleAnnotationArrayType = Type.getObjectType("[${annotationArrayType.descriptor}")
internal val stringType = AsmTypes.JAVA_STRING_TYPE
internal val stringArrayType = Type.getObjectType("[${stringType.descriptor}")

fun InstructionAdapter.wrapStackValueIntoNullableSerializer() =
    invokestatic(
        "kotlinx/serialization/builtins/BuiltinSerializersKt", "getNullable",
        "(" + kSerializerType.descriptor + ")" + kSerializerType.descriptor, false
    )

fun <T> InstructionAdapter.fillArray(type: Type, args: List<T>, onEach: (Int, T) -> Unit) {
    iconst(args.size)
    newarray(type)
    args.forEachIndexed { i, arg ->
        dup()
        iconst(i)
        onEach(i, arg)
        astore(type)
    }
}
