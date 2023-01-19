/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.internal

import kotlinx.metadata.ClassName
import kotlinx.metadata.KmAnnotation
import kotlinx.metadata.KmAnnotationArgument
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf.Annotation.Argument.Value.Type.*
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.NameResolver

fun ProtoBuf.Annotation.readAnnotation(strings: NameResolver): KmAnnotation =
    KmAnnotation(
        strings.getClassName(id),
        argumentList.mapNotNull { argument ->
            argument.value.readAnnotationArgument(strings)?.let { value ->
                strings.getString(argument.nameId) to value
            }
        }.toMap()
    )

fun ProtoBuf.Annotation.Argument.Value.readAnnotationArgument(strings: NameResolver): KmAnnotationArgument? {
    if (Flags.IS_UNSIGNED[flags]) {
        return when (type) {
            BYTE -> KmAnnotationArgument.UByteValue(intValue.toByte().toUByte())
            SHORT -> KmAnnotationArgument.UShortValue(intValue.toShort().toUShort())
            INT -> KmAnnotationArgument.UIntValue(intValue.toInt().toUInt())
            LONG -> KmAnnotationArgument.ULongValue(intValue.toULong())
            else -> error("Cannot read value of unsigned type: $type")
        }
    }

    return when (type) {
        BYTE -> KmAnnotationArgument.ByteValue(intValue.toByte())
        CHAR -> KmAnnotationArgument.CharValue(intValue.toInt().toChar())
        SHORT -> KmAnnotationArgument.ShortValue(intValue.toShort())
        INT -> KmAnnotationArgument.IntValue(intValue.toInt())
        LONG -> KmAnnotationArgument.LongValue(intValue)
        FLOAT -> KmAnnotationArgument.FloatValue(floatValue)
        DOUBLE -> KmAnnotationArgument.DoubleValue(doubleValue)
        BOOLEAN -> KmAnnotationArgument.BooleanValue(intValue != 0L)
        STRING -> KmAnnotationArgument.StringValue(strings.getString(stringValue))
        CLASS -> strings.getClassName(classId).let { className ->
            if (arrayDimensionCount == 0)
                KmAnnotationArgument.KClassValue(className)
            else
                KmAnnotationArgument.ArrayKClassValue(className, arrayDimensionCount)
        }
        ENUM -> KmAnnotationArgument.EnumValue(strings.getClassName(classId), strings.getString(enumValueId))
        ANNOTATION -> KmAnnotationArgument.AnnotationValue(annotation.readAnnotation(strings))
        ARRAY -> KmAnnotationArgument.ArrayValue(arrayElementList.mapNotNull { it.readAnnotationArgument(strings) })
        null -> null
    }
}

internal fun NameResolver.getClassName(index: Int): ClassName {
    val name = getQualifiedClassName(index)
    return if (isLocalClassName(index)) ".$name" else name
}
