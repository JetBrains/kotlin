/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.metadata

/**
 * Represents an annotation, written to the Kotlin metadata. Note that not all annotations are written to metadata on all platforms.
 * For example, on JVM most of the annotations are written directly on the corresponding declarations in the class file,
 * and entries in the metadata only have an attribute (such as [KmClass.hasAnnotations]) to signal if they do have annotations in the bytecode.
 * On JVM, only annotations on type parameters and types are serialized to the Kotlin metadata
 * (see `KmType.annotations` and `KmTypeParameter.annotations` JVM extensions)
 *
 * @property className The fully qualified name of the annotation class
 * @property arguments Explicitly specified arguments to the annotation; does not include default values for annotation parameters
 * (specified in the annotation class declaration)
 */
public class KmAnnotation(public val className: ClassName, public val arguments: Map<String, KmAnnotationArgument>) {

    /**
     * Checks if this KmAnnotation is equal to the [other].
     * Instances of KmAnnotation are equal if they have same [className] and [arguments].
     */
    override fun equals(other: Any?): Boolean =
        this === other || other is KmAnnotation && className == other.className && arguments == other.arguments


    /**
     * Returns hash code of this instance.
     * Hash code is computed based on [className] and [arguments].
     */
    override fun hashCode(): Int = 31 * className.hashCode() + arguments.hashCode()

    /**
     * Returns string representation of this instance with `@` sign, [className], and [arguments] in parentheses.
     */
    override fun toString(): String {
        val args = arguments.toList().joinToString { (k, v) -> "$k = $v" }
        return "@$className($args)"
    }
}

/**
 * Represents an argument of the annotation.
 */
@Suppress("IncorrectFormatting") // one-line KDoc
public sealed class KmAnnotationArgument {

    // Avoid triggering Dokka configured for failing on undocumented functions
    /** @suppress */
    abstract override fun toString(): String

    /**
     * A kind of annotation argument, whose value is directly accessible via [value].
     * This is possible for annotation arguments of primitive types, unsigned types, and strings.
     *
     * For example, in `@Foo("bar")`, argument of `Foo` is a [StringValue] with [value] equal to `bar`.
     *
     * @param T the type of the value of this argument
     */
    public sealed class LiteralValue<out T : Any> : KmAnnotationArgument() {
        /**
         * The value of this argument.
         */
        public abstract val value: T

        // the final modifier prevents generation of data class-like .toString() in inheritors
        // Java reflection instead of Kotlin reflection to avoid (probably small) overhead of mapping Kotlin/Java names
        final override fun toString(): String =
            "${this::class.java.simpleName}(${if (this is StringValue) "\"$value\"" else value.toString()})"
    }

    // For all inheritors of LiteralValue: KDoc is automatically copied from base property `value`
    // to the overridden one. However, it does not do this with classes, and we do not have `@inheritdoc` :(

    /** An annotation argument with a [Byte] type. */
    public data class ByteValue(override val value: Byte) : LiteralValue<Byte>()
    /** An annotation argument with a [Char] type. */
    public data class CharValue(override val value: Char) : LiteralValue<Char>()
    /** An annotation argument with a [Short] type. */
    public data class ShortValue(override val value: Short) : LiteralValue<Short>()
    /** An annotation argument with a [Int] type. */
    public data class IntValue(override val value: Int) : LiteralValue<Int>()
    /** An annotation argument with a [Long] type. */
    public data class LongValue(override val value: Long) : LiteralValue<Long>()
    /** An annotation argument with a [Float] type. */
    public data class FloatValue(override val value: Float) : LiteralValue<Float>()
    /** An annotation argument with a [Double] type. */
    public data class DoubleValue(override val value: Double) : LiteralValue<Double>()
    /** An annotation argument with a [Boolean] type. */
    public data class BooleanValue(override val value: Boolean) : LiteralValue<Boolean>()

    /** An annotation argument with a [UByte] type. */
    public data class UByteValue(override val value: UByte) : LiteralValue<UByte>()
    /** An annotation argument with a [UShort] type. */
    public data class UShortValue(override val value: UShort) : LiteralValue<UShort>()
    /** An annotation argument with a [UInt] type. */
    public data class UIntValue(override val value: UInt) : LiteralValue<UInt>()
    /** An annotation argument with a [ULong] type. */
    public data class ULongValue(override val value: ULong) : LiteralValue<ULong>()

    /** An annotation argument with a [String] type. */
    public data class StringValue(override val value: String) : LiteralValue<String>()

    /**
     * An annotation argument with an enumeration type.
     *
     * For example, in `@Foo(MyEnum.OPTION_A)`, argument of `Foo` is an `EnumValue`
     * with [enumClassName] `MyEnum` and [enumEntryName] `OPTION_A`.
     *
     * @property enumClassName FQ name of the enum class
     * @property enumEntryName Name of the enum entry
     */
    public data class EnumValue(val enumClassName: ClassName, val enumEntryName: String) : KmAnnotationArgument() {
        override fun toString(): String = "EnumValue($enumClassName.$enumEntryName)"
    }

    /**
     * An annotation argument which is another annotation value.
     *
     * For example, with the following classes:
     * ```
     * annotation class Bar(val s: String)
     *
     * annotation class Foo(val b: Bar)
     * ```
     * It is possible to apply such annotation: `@Foo(Bar("baz"))`. In this case, argument `Foo.b` is represented by
     * `AnnotationValue` which [annotation] property contains all necessary information: the fact that it is a `Bar` annotation ([KmAnnotation.className])
     * and that it has a "baz" argument ([KmAnnotation.arguments]).
     *
     * @property annotation Annotation instance with all its arguments.
     */
    public data class AnnotationValue(val annotation: KmAnnotation) : KmAnnotationArgument() {
        override fun toString(): String = "AnnotationValue($annotation)"
    }

    /**
     * An annotation argument with an array type, i.e., several values of one arbitrary type.
     *
     * For example, in `@Foo(["a", "b", "c"])` argument of `Foo` is an `ArrayValue` with [elements]
     * being a list of three [StringValue] elements: "a", "b", and "c" (without quotes).
     *
     * Don't confuse with [ArrayKClassValue], which represents KClass value.
     *
     * @property elements Values of elements in the array.
     */
    public data class ArrayValue(val elements: List<KmAnnotationArgument>) : KmAnnotationArgument() {
        override fun toString(): String = "ArrayValue($elements)"
    }

    /**
     * An annotation argument of KClass type.
     *
     * For example, in `@Foo(String::class)` argument of `Foo` is a `KClassValue` which [className] is `kotlin/String`.
     *
     * All the KClasses, except `kotlin.Array`, are represented by this class.
     * Arrays are a specific case and represented by [ArrayKClassValue] — see its documentation for details.
     *
     * @property className FQ name of the referenced class.
     */
    public data class KClassValue(val className: ClassName) : KmAnnotationArgument() {
        override fun toString(): String = "KClassValue($className)"
    }

    /**
     * Annotation argument whose type is one of the `kotlin.Array` KClasses.
     *
     * Due to the nature of JVM, Arrays with different arguments are represented by different `kotlin.reflect.KClass` and `java.lang.Class` instances
     * (while e.g. `List` has always one KClass instance regardless of generic arguments).
     * As a result, Kotlin compiler allows using generic arguments for the arrays in annotations: `@Foo(Array<Array<String>>::class)`.
     * [ArrayKClassValue] allows to distinguish such arguments from regular [KClassValue].
     *
     * [className] is the array element type's fully qualified name — in the example above, it is `kotlin/String`.
     * [arrayDimensionCount] is the dimension of the array — 1 for `Array<String>::class`, 2 for `Array<Array<String>>::class`, and so on.
     * It is guaranteed to be at least 1.
     *
     * For platforms other than JVM, the value for [className] is always `kotlin/Any` and [arrayDimensionCount] is always 1.
     * This represents untyped `Array::class` expression.
     *
     * See also: https://youtrack.jetbrains.com/issue/KT-31230
     *
     * @property className FQ name of the referenced array element type.
     * @property arrayDimensionCount Referenced array dimension.
     */
    public data class ArrayKClassValue(val className: ClassName, val arrayDimensionCount: Int) : KmAnnotationArgument() {
        init {
            require(arrayDimensionCount > 0) { "ArrayKClassValue must have at least one dimension. For regular X::class argument, use KClassValue." }
        }

        private val stringRepresentation = buildString {
            append("ArrayKClassValue(")
            repeat(arrayDimensionCount) { append("kotlin/Array<") }
            append(className)
            repeat(arrayDimensionCount) { append(">") }
            append(")")
        }

        override fun toString(): String = stringRepresentation
    }
}
