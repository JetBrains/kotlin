/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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
data class KmAnnotation(val className: ClassName, val arguments: Map<String, KmAnnotationArgument<*>>)

/**
 * Represents an argument to the annotation.
 *
 * @param T the type of the value of this argument
 */
sealed class KmAnnotationArgument<out T : Any> {
    /**
     * The value of this argument.
     */
    abstract val value: T

    data class ByteValue(override val value: Byte) : KmAnnotationArgument<Byte>()
    data class CharValue(override val value: Char) : KmAnnotationArgument<Char>()
    data class ShortValue(override val value: Short) : KmAnnotationArgument<Short>()
    data class IntValue(override val value: Int) : KmAnnotationArgument<Int>()
    data class LongValue(override val value: Long) : KmAnnotationArgument<Long>()
    data class FloatValue(override val value: Float) : KmAnnotationArgument<Float>()
    data class DoubleValue(override val value: Double) : KmAnnotationArgument<Double>()
    data class BooleanValue(override val value: Boolean) : KmAnnotationArgument<Boolean>()

    data class UByteValue(override val value: Byte) : KmAnnotationArgument<Byte>()
    data class UShortValue(override val value: Short) : KmAnnotationArgument<Short>()
    data class UIntValue(override val value: Int) : KmAnnotationArgument<Int>()
    data class ULongValue(override val value: Long) : KmAnnotationArgument<Long>()

    data class StringValue(override val value: String) : KmAnnotationArgument<String>()
    data class KClassValue(override val value: ClassName) : KmAnnotationArgument<ClassName>()
    data class EnumValue(val enumClassName: ClassName, val enumEntryName: String) : KmAnnotationArgument<String>() {
        override val value: String = "$enumClassName.$enumEntryName"
    }

    data class AnnotationValue(override val value: KmAnnotation) : KmAnnotationArgument<KmAnnotation>()
    data class ArrayValue(override val value: List<KmAnnotationArgument<*>>) : KmAnnotationArgument<List<KmAnnotationArgument<*>>>()
}
