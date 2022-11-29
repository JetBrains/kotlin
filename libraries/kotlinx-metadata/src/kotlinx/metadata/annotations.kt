/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata

/**
 * Represents an annotation, written to the Kotlin metadata. Note that not all annotations are written to metadata on all platforms.
 * For example, on JVM most of the annotations are written directly on the corresponding declarations in the class file,
 * and entries in the metadata only have a flag ([Flag.HAS_ANNOTATIONS]) to signal if they do have annotations in the bytecode.
 * On JVM, only annotations on type parameters and types are serialized to the Kotlin metadata.
 *
 * @param className the fully qualified name of the annotation class
 * @param arguments explicitly specified arguments to the annotation; does not include default values for annotation parameters
 *                  (specified in the annotation class declaration)
 */
data class KmAnnotation(val className: ClassName, val arguments: Map<String, KmAnnotationArgument>)

/**
 * Represents an argument to the annotation.
 */
sealed class KmAnnotationArgument {
    /**
     * A kind of annotation argument, whose value is directly accessible via [value].
     * This is possible for annotation arguments of primitive types, unsigned types and strings.
     *
     * @param T the type of the value of this argument
     */
    sealed class LiteralValue<out T : Any> : KmAnnotationArgument() {
        /**
         * The value of this argument.
         */
        abstract val value: T
    }

    data class ByteValue(override val value: Byte) : LiteralValue<Byte>()
    data class CharValue(override val value: Char) : LiteralValue<Char>()
    data class ShortValue(override val value: Short) : LiteralValue<Short>()
    data class IntValue(override val value: Int) : LiteralValue<Int>()
    data class LongValue(override val value: Long) : LiteralValue<Long>()
    data class FloatValue(override val value: Float) : LiteralValue<Float>()
    data class DoubleValue(override val value: Double) : LiteralValue<Double>()
    data class BooleanValue(override val value: Boolean) : LiteralValue<Boolean>()

    data class UByteValue(override val value: UByte) : LiteralValue<UByte>()

    data class UShortValue(override val value: UShort) : LiteralValue<UShort>()

    data class UIntValue(override val value: UInt) : LiteralValue<UInt>()

    data class ULongValue(override val value: ULong) : LiteralValue<ULong>()

    data class StringValue(override val value: String) : LiteralValue<String>()

    data class KClassValue(val className: ClassName, val arrayDimensionCount: Int) : KmAnnotationArgument()

    data class EnumValue(val enumClassName: ClassName, val enumEntryName: String) : KmAnnotationArgument()

    data class AnnotationValue(val annotation: KmAnnotation) : KmAnnotationArgument()
    data class ArrayValue(val elements: List<KmAnnotationArgument>) : KmAnnotationArgument()
}
