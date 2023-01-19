/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.internal

import kotlinx.metadata.ClassName
import kotlinx.metadata.KmAnnotation
import kotlinx.metadata.KmAnnotationArgument
import kotlinx.metadata.isLocalClassName
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.serialization.StringTable

fun KmAnnotation.writeAnnotation(strings: StringTable): ProtoBuf.Annotation.Builder =
    ProtoBuf.Annotation.newBuilder().apply {
        id = strings.getClassNameIndex(className)
        for ((name, argument) in arguments) {
            addArgument(ProtoBuf.Annotation.Argument.newBuilder().apply {
                nameId = strings.getStringIndex(name)
                value = argument.writeAnnotationArgument(strings).build()
            })
        }
    }

fun KmAnnotationArgument.writeAnnotationArgument(strings: StringTable): ProtoBuf.Annotation.Argument.Value.Builder =
    ProtoBuf.Annotation.Argument.Value.newBuilder().apply {
        when (this@writeAnnotationArgument) {
            is KmAnnotationArgument.ByteValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.BYTE
                this.intValue = value.toLong()
            }
            is KmAnnotationArgument.CharValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.CHAR
                this.intValue = value.code.toLong()
            }
            is KmAnnotationArgument.ShortValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.SHORT
                this.intValue = value.toLong()
            }
            is KmAnnotationArgument.IntValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.INT
                this.intValue = value.toLong()
            }
            is KmAnnotationArgument.LongValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.LONG
                this.intValue = value
            }
            is KmAnnotationArgument.FloatValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.FLOAT
                this.floatValue = value
            }
            is KmAnnotationArgument.DoubleValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.DOUBLE
                this.doubleValue = value
            }
            is KmAnnotationArgument.BooleanValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.BOOLEAN
                this.intValue = if (value) 1 else 0
            }
            is KmAnnotationArgument.UByteValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.BYTE
                this.intValue = value.toLong()
                this.flags = Flags.IS_UNSIGNED.toFlags(true)
            }
            is KmAnnotationArgument.UShortValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.SHORT
                this.intValue = value.toLong()
                this.flags = Flags.IS_UNSIGNED.toFlags(true)
            }
            is KmAnnotationArgument.UIntValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.INT
                this.intValue = value.toLong()
                this.flags = Flags.IS_UNSIGNED.toFlags(true)
            }
            is KmAnnotationArgument.ULongValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.LONG
                this.intValue = value.toLong()
                this.flags = Flags.IS_UNSIGNED.toFlags(true)
            }
            is KmAnnotationArgument.StringValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.STRING
                this.stringValue = strings.getStringIndex(value)
            }
            is KmAnnotationArgument.KClassValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.CLASS
                this.classId = strings.getClassNameIndex(className)
            }
            is KmAnnotationArgument.ArrayKClassValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.CLASS
                this.classId = strings.getClassNameIndex(className)
                this.arrayDimensionCount = this@writeAnnotationArgument.arrayDimensionCount
            }
            is KmAnnotationArgument.EnumValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.ENUM
                this.classId = strings.getClassNameIndex(enumClassName)
                this.enumValueId = strings.getStringIndex(enumEntryName)
            }
            is KmAnnotationArgument.AnnotationValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.ANNOTATION
                this.annotation = this@writeAnnotationArgument.annotation.writeAnnotation(strings).build()
            }
            is KmAnnotationArgument.ArrayValue -> {
                this.type = ProtoBuf.Annotation.Argument.Value.Type.ARRAY
                for (element in elements) {
                    this.addArrayElement(element.writeAnnotationArgument(strings))
                }
            }
        }
    }

internal fun StringTable.getClassNameIndex(name: ClassName): Int =
    if (name.isLocalClassName())
        getQualifiedClassNameIndex(name.substring(1), true)
    else
        getQualifiedClassNameIndex(name, false)
