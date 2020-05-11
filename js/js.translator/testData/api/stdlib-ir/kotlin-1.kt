@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY}) @kotlin.annotation.Retention(value = AnnotationRetention.BINARY) @kotlin.annotation.MustBeDocumented @kotlin.SinceKotlin(version = "1.1") public final annotation class PublishedApi : kotlin.Annotation {
    /*primary*/ public constructor PublishedApi()
}

@kotlin.annotation.Target(allowedTargets = {}) @kotlin.annotation.Retention(value = AnnotationRetention.BINARY) @kotlin.annotation.MustBeDocumented public final annotation class ReplaceWith : kotlin.Annotation {
    /*primary*/ public constructor ReplaceWith(/*0*/ expression: kotlin.String, /*1*/ vararg imports: kotlin.String /*kotlin.Array<out kotlin.String>*/)
    public final val expression: kotlin.String
        public final fun <get-expression>(): kotlin.String
    public final val imports: kotlin.Array<out kotlin.String>
        public final fun <get-imports>(): kotlin.Array<out kotlin.String>
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.ANNOTATION_CLASS}) @kotlin.annotation.Retention(value = AnnotationRetention.BINARY) @kotlin.SinceKotlin(version = "1.3") public final annotation class RequiresOptIn : kotlin.Annotation {
    /*primary*/ public constructor RequiresOptIn(/*0*/ message: kotlin.String = ..., /*1*/ level: kotlin.RequiresOptIn.Level = ...)
    public final val level: kotlin.RequiresOptIn.Level
        public final fun <get-level>(): kotlin.RequiresOptIn.Level
    public final val message: kotlin.String
        public final fun <get-message>(): kotlin.String

    public final enum class Level : kotlin.Enum<kotlin.RequiresOptIn.Level> {
        enum entry WARNING

        enum entry ERROR

        // Static members
        public final /*synthesized*/ fun valueOf(/*0*/ value: kotlin.String): kotlin.RequiresOptIn.Level
        public final /*synthesized*/ fun values(): kotlin.Array<kotlin.RequiresOptIn.Level>
    }
}

@kotlin.SinceKotlin(version = "1.3") public final inline class Result</*0*/ out T> : kotlin.io.Serializable {
    public final val isFailure: kotlin.Boolean
        public final fun <get-isFailure>(): kotlin.Boolean
    public final val isSuccess: kotlin.Boolean
        public final fun <get-isSuccess>(): kotlin.Boolean
    public open override /*1*/ /*synthesized*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public final fun exceptionOrNull(): kotlin.Throwable?
    @kotlin.internal.InlineOnly public final inline fun getOrNull(): T?
    public open override /*1*/ /*synthesized*/ fun hashCode(): kotlin.Int
    public open override /*1*/ fun toString(): kotlin.String

    public companion object Companion {
        @kotlin.internal.InlineOnly public final inline fun </*0*/ T> failure(/*0*/ exception: kotlin.Throwable): kotlin.Result<T>
        @kotlin.internal.InlineOnly public final inline fun </*0*/ T> success(/*0*/ value: T): kotlin.Result<T>
    }
}

public open class RuntimeException : kotlin.Exception {
    public constructor RuntimeException()
    public constructor RuntimeException(/*0*/ message: kotlin.String?)
    public constructor RuntimeException(/*0*/ message: kotlin.String?, /*1*/ cause: kotlin.Throwable?)
    public constructor RuntimeException(/*0*/ cause: kotlin.Throwable?)
}

public final class Short : kotlin.Number, kotlin.Comparable<kotlin.Short> {
    public final operator fun compareTo(/*0*/ other: kotlin.Byte): kotlin.Int
    public final operator fun compareTo(/*0*/ other: kotlin.Double): kotlin.Int
    public final operator fun compareTo(/*0*/ other: kotlin.Float): kotlin.Int
    public final operator fun compareTo(/*0*/ other: kotlin.Int): kotlin.Int
    public final operator fun compareTo(/*0*/ other: kotlin.Long): kotlin.Int
    public open override /*1*/ fun compareTo(/*0*/ other: kotlin.Short): kotlin.Int
    public final operator fun dec(): kotlin.Short
    public final operator fun div(/*0*/ other: kotlin.Byte): kotlin.Int
    public final operator fun div(/*0*/ other: kotlin.Double): kotlin.Double
    public final operator fun div(/*0*/ other: kotlin.Float): kotlin.Float
    public final operator fun div(/*0*/ other: kotlin.Int): kotlin.Int
    public final operator fun div(/*0*/ other: kotlin.Long): kotlin.Long
    public final operator fun div(/*0*/ other: kotlin.Short): kotlin.Int
    public open override /*2*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public open override /*2*/ fun hashCode(): kotlin.Int
    public final operator fun inc(): kotlin.Short
    public final operator fun minus(/*0*/ other: kotlin.Byte): kotlin.Int
    public final operator fun minus(/*0*/ other: kotlin.Double): kotlin.Double
    public final operator fun minus(/*0*/ other: kotlin.Float): kotlin.Float
    public final operator fun minus(/*0*/ other: kotlin.Int): kotlin.Int
    public final operator fun minus(/*0*/ other: kotlin.Long): kotlin.Long
    public final operator fun minus(/*0*/ other: kotlin.Short): kotlin.Int
    public final operator fun plus(/*0*/ other: kotlin.Byte): kotlin.Int
    public final operator fun plus(/*0*/ other: kotlin.Double): kotlin.Double
    public final operator fun plus(/*0*/ other: kotlin.Float): kotlin.Float
    public final operator fun plus(/*0*/ other: kotlin.Int): kotlin.Int
    public final operator fun plus(/*0*/ other: kotlin.Long): kotlin.Long
    public final operator fun plus(/*0*/ other: kotlin.Short): kotlin.Int
    public final operator fun rangeTo(/*0*/ other: kotlin.Byte): kotlin.ranges.IntRange
    public final operator fun rangeTo(/*0*/ other: kotlin.Int): kotlin.ranges.IntRange
    public final operator fun rangeTo(/*0*/ other: kotlin.Long): kotlin.ranges.LongRange
    public final operator fun rangeTo(/*0*/ other: kotlin.Short): kotlin.ranges.IntRange
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Byte): kotlin.Int
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Double): kotlin.Double
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Float): kotlin.Float
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Int): kotlin.Int
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Long): kotlin.Long
    @kotlin.SinceKotlin(version = "1.1") public final operator fun rem(/*0*/ other: kotlin.Short): kotlin.Int
    public final operator fun times(/*0*/ other: kotlin.Byte): kotlin.Int
    public final operator fun times(/*0*/ other: kotlin.Double): kotlin.Double
    public final operator fun times(/*0*/ other: kotlin.Float): kotlin.Float
    public final operator fun times(/*0*/ other: kotlin.Int): kotlin.Int
    public final operator fun times(/*0*/ other: kotlin.Long): kotlin.Long
    public final operator fun times(/*0*/ other: kotlin.Short): kotlin.Int
    public open override /*1*/ fun toByte(): kotlin.Byte
    public open override /*1*/ fun toChar(): kotlin.Char
    public open override /*1*/ fun toDouble(): kotlin.Double
    public open override /*1*/ fun toFloat(): kotlin.Float
    public open override /*1*/ fun toInt(): kotlin.Int
    public open override /*1*/ fun toLong(): kotlin.Long
    public open override /*1*/ fun toShort(): kotlin.Short
    public open override /*2*/ fun toString(): kotlin.String
    public final operator fun unaryMinus(): kotlin.Int
    public final operator fun unaryPlus(): kotlin.Int

    public companion object Companion {
        public const final val MAX_VALUE: kotlin.Short = 32767.toShort()
            public final fun <get-MAX_VALUE>(): kotlin.Short
        public const final val MIN_VALUE: kotlin.Short = -32768.toShort()
            public final fun <get-MIN_VALUE>(): kotlin.Short
        @kotlin.SinceKotlin(version = "1.3") public const final val SIZE_BITS: kotlin.Int = 16
            public final fun <get-SIZE_BITS>(): kotlin.Int
        @kotlin.SinceKotlin(version = "1.3") public const final val SIZE_BYTES: kotlin.Int = 2
            public final fun <get-SIZE_BYTES>(): kotlin.Int
    }
}

public final class ShortArray {
    /*primary*/ public constructor ShortArray(/*0*/ size: kotlin.Int)
    public constructor ShortArray(/*0*/ size: kotlin.Int, /*1*/ init: (kotlin.Int) -> kotlin.Short)
    public final val size: kotlin.Int
        public final fun <get-size>(): kotlin.Int
    public final operator fun get(/*0*/ index: kotlin.Int): kotlin.Short
    public final operator fun iterator(): kotlin.collections.ShortIterator
    public final operator fun set(/*0*/ index: kotlin.Int, /*1*/ value: kotlin.Short): kotlin.Unit
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.TYPEALIAS}) @kotlin.annotation.Retention(value = AnnotationRetention.BINARY) @kotlin.annotation.MustBeDocumented public final annotation class SinceKotlin : kotlin.Annotation {
    /*primary*/ public constructor SinceKotlin(/*0*/ version: kotlin.String)
    public final val version: kotlin.String
        public final fun <get-version>(): kotlin.String
}

public final class String : kotlin.Comparable<kotlin.String>, kotlin.CharSequence {
    /*primary*/ public constructor String()
    public open override /*1*/ val length: kotlin.Int
        public open override /*1*/ fun <get-length>(): kotlin.Int
    public open override /*1*/ fun compareTo(/*0*/ other: kotlin.String): kotlin.Int
    public open override /*2*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public open override /*1*/ fun get(/*0*/ index: kotlin.Int): kotlin.Char
    public open override /*2*/ fun hashCode(): kotlin.Int
    public final operator fun plus(/*0*/ other: kotlin.Any?): kotlin.String
    public open override /*1*/ fun subSequence(/*0*/ startIndex: kotlin.Int, /*1*/ endIndex: kotlin.Int): kotlin.CharSequence
    public open override /*2*/ fun toString(): kotlin.String

    public companion object Companion {
    }
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.TYPE, AnnotationTarget.EXPRESSION, AnnotationTarget.FILE, AnnotationTarget.TYPEALIAS}) @kotlin.annotation.Retention(value = AnnotationRetention.SOURCE) public final annotation class Suppress : kotlin.Annotation {
    /*primary*/ public constructor Suppress(/*0*/ vararg names: kotlin.String /*kotlin.Array<out kotlin.String>*/)
    public final val names: kotlin.Array<out kotlin.String>
        public final fun <get-names>(): kotlin.Array<out kotlin.String>
}

@kotlin.js.JsName(name = "Error") public open external class Throwable {
    public constructor Throwable()
    public constructor Throwable(/*0*/ message: kotlin.String?)
    public constructor Throwable(/*0*/ message: kotlin.String?, /*1*/ cause: kotlin.Throwable?)
    public constructor Throwable(/*0*/ cause: kotlin.Throwable?)
    public open val cause: kotlin.Throwable?
        public open fun <get-cause>(): kotlin.Throwable?
    public open val message: kotlin.String?
        public open fun <get-message>(): kotlin.String?
    public open override /*1*/ fun toString(): kotlin.String
}

public final data class Triple</*0*/ out A, /*1*/ out B, /*2*/ out C> : kotlin.io.Serializable {
    /*primary*/ public constructor Triple</*0*/ out A, /*1*/ out B, /*2*/ out C>(/*0*/ first: A, /*1*/ second: B, /*2*/ third: C)
    public final val first: A
        public final fun <get-first>(): A
    public final val second: B
        public final fun <get-second>(): B
    public final val third: C
        public final fun <get-third>(): C
    public final operator /*synthesized*/ fun component1(): A
    public final operator /*synthesized*/ fun component2(): B
    public final operator /*synthesized*/ fun component3(): C
    public final /*synthesized*/ fun copy(/*0*/ first: A = ..., /*1*/ second: B = ..., /*2*/ third: C = ...): kotlin.Triple<A, B, C>
    public open override /*1*/ /*synthesized*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public open override /*1*/ /*synthesized*/ fun hashCode(): kotlin.Int
    public open override /*1*/ fun toString(): kotlin.String
}

@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public final inline class UByte : kotlin.Comparable<kotlin.UByte> {
    @kotlin.internal.InlineOnly public final inline infix fun and(/*0*/ other: kotlin.UByte): kotlin.UByte
    @kotlin.internal.InlineOnly public open override /*1*/ inline fun compareTo(/*0*/ other: kotlin.UByte): kotlin.Int
    @kotlin.internal.InlineOnly public final inline operator fun compareTo(/*0*/ other: kotlin.UInt): kotlin.Int
    @kotlin.internal.InlineOnly public final inline operator fun compareTo(/*0*/ other: kotlin.ULong): kotlin.Int
    @kotlin.internal.InlineOnly public final inline operator fun compareTo(/*0*/ other: kotlin.UShort): kotlin.Int
    @kotlin.internal.InlineOnly public final inline operator fun dec(): kotlin.UByte
    @kotlin.internal.InlineOnly public final inline operator fun div(/*0*/ other: kotlin.UByte): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun div(/*0*/ other: kotlin.UInt): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun div(/*0*/ other: kotlin.ULong): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun div(/*0*/ other: kotlin.UShort): kotlin.UInt
    public open override /*1*/ /*synthesized*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public open override /*1*/ /*synthesized*/ fun hashCode(): kotlin.Int
    @kotlin.internal.InlineOnly public final inline operator fun inc(): kotlin.UByte
    @kotlin.internal.InlineOnly public final inline fun inv(): kotlin.UByte
    @kotlin.internal.InlineOnly public final inline operator fun minus(/*0*/ other: kotlin.UByte): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun minus(/*0*/ other: kotlin.UInt): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun minus(/*0*/ other: kotlin.ULong): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun minus(/*0*/ other: kotlin.UShort): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline infix fun or(/*0*/ other: kotlin.UByte): kotlin.UByte
    @kotlin.internal.InlineOnly public final inline operator fun plus(/*0*/ other: kotlin.UByte): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun plus(/*0*/ other: kotlin.UInt): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun plus(/*0*/ other: kotlin.ULong): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun plus(/*0*/ other: kotlin.UShort): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun rangeTo(/*0*/ other: kotlin.UByte): kotlin.ranges.UIntRange
    @kotlin.internal.InlineOnly public final inline operator fun rem(/*0*/ other: kotlin.UByte): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun rem(/*0*/ other: kotlin.UInt): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun rem(/*0*/ other: kotlin.ULong): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun rem(/*0*/ other: kotlin.UShort): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun times(/*0*/ other: kotlin.UByte): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun times(/*0*/ other: kotlin.UInt): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun times(/*0*/ other: kotlin.ULong): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun times(/*0*/ other: kotlin.UShort): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline fun toByte(): kotlin.Byte
    @kotlin.internal.InlineOnly public final inline fun toDouble(): kotlin.Double
    @kotlin.internal.InlineOnly public final inline fun toFloat(): kotlin.Float
    @kotlin.internal.InlineOnly public final inline fun toInt(): kotlin.Int
    @kotlin.internal.InlineOnly public final inline fun toLong(): kotlin.Long
    @kotlin.internal.InlineOnly public final inline fun toShort(): kotlin.Short
    public open override /*1*/ fun toString(): kotlin.String
    @kotlin.internal.InlineOnly public final inline fun toUByte(): kotlin.UByte
    @kotlin.internal.InlineOnly public final inline fun toUInt(): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline fun toULong(): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline fun toUShort(): kotlin.UShort
    @kotlin.internal.InlineOnly public final inline infix fun xor(/*0*/ other: kotlin.UByte): kotlin.UByte

    public companion object Companion {
        public const final val MAX_VALUE: kotlin.UByte = -1.toUByte()
            public final fun <get-MAX_VALUE>(): kotlin.UByte
        public const final val MIN_VALUE: kotlin.UByte = 0.toUByte()
            public final fun <get-MIN_VALUE>(): kotlin.UByte
        public const final val SIZE_BITS: kotlin.Int = 8
            public final fun <get-SIZE_BITS>(): kotlin.Int
        public const final val SIZE_BYTES: kotlin.Int = 1
            public final fun <get-SIZE_BYTES>(): kotlin.Int
    }
}

@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public final inline class UByteArray : kotlin.collections.Collection<kotlin.UByte> {
    public constructor UByteArray(/*0*/ size: kotlin.Int)
    public open override /*1*/ val size: kotlin.Int
        public open override /*1*/ fun <get-size>(): kotlin.Int
    public open override /*1*/ fun contains(/*0*/ element: kotlin.UByte): kotlin.Boolean
    public open override /*1*/ fun containsAll(/*0*/ elements: kotlin.collections.Collection<kotlin.UByte>): kotlin.Boolean
    public open override /*1*/ /*synthesized*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public final operator fun get(/*0*/ index: kotlin.Int): kotlin.UByte
    public open override /*1*/ /*synthesized*/ fun hashCode(): kotlin.Int
    public open override /*1*/ fun isEmpty(): kotlin.Boolean
    public open override /*1*/ fun iterator(): kotlin.collections.UByteIterator
    public final operator fun set(/*0*/ index: kotlin.Int, /*1*/ value: kotlin.UByte): kotlin.Unit
    public open override /*1*/ /*synthesized*/ fun toString(): kotlin.String
}

@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public final inline class UInt : kotlin.Comparable<kotlin.UInt> {
    @kotlin.internal.InlineOnly public final inline infix fun and(/*0*/ other: kotlin.UInt): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun compareTo(/*0*/ other: kotlin.UByte): kotlin.Int
    @kotlin.internal.InlineOnly public open override /*1*/ inline fun compareTo(/*0*/ other: kotlin.UInt): kotlin.Int
    @kotlin.internal.InlineOnly public final inline operator fun compareTo(/*0*/ other: kotlin.ULong): kotlin.Int
    @kotlin.internal.InlineOnly public final inline operator fun compareTo(/*0*/ other: kotlin.UShort): kotlin.Int
    @kotlin.internal.InlineOnly public final inline operator fun dec(): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun div(/*0*/ other: kotlin.UByte): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun div(/*0*/ other: kotlin.UInt): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun div(/*0*/ other: kotlin.ULong): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun div(/*0*/ other: kotlin.UShort): kotlin.UInt
    public open override /*1*/ /*synthesized*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public open override /*1*/ /*synthesized*/ fun hashCode(): kotlin.Int
    @kotlin.internal.InlineOnly public final inline operator fun inc(): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline fun inv(): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun minus(/*0*/ other: kotlin.UByte): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun minus(/*0*/ other: kotlin.UInt): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun minus(/*0*/ other: kotlin.ULong): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun minus(/*0*/ other: kotlin.UShort): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline infix fun or(/*0*/ other: kotlin.UInt): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun plus(/*0*/ other: kotlin.UByte): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun plus(/*0*/ other: kotlin.UInt): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun plus(/*0*/ other: kotlin.ULong): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun plus(/*0*/ other: kotlin.UShort): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun rangeTo(/*0*/ other: kotlin.UInt): kotlin.ranges.UIntRange
    @kotlin.internal.InlineOnly public final inline operator fun rem(/*0*/ other: kotlin.UByte): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun rem(/*0*/ other: kotlin.UInt): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun rem(/*0*/ other: kotlin.ULong): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun rem(/*0*/ other: kotlin.UShort): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline infix fun shl(/*0*/ bitCount: kotlin.Int): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline infix fun shr(/*0*/ bitCount: kotlin.Int): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun times(/*0*/ other: kotlin.UByte): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun times(/*0*/ other: kotlin.UInt): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun times(/*0*/ other: kotlin.ULong): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun times(/*0*/ other: kotlin.UShort): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline fun toByte(): kotlin.Byte
    @kotlin.internal.InlineOnly public final inline fun toDouble(): kotlin.Double
    @kotlin.internal.InlineOnly public final inline fun toFloat(): kotlin.Float
    @kotlin.internal.InlineOnly public final inline fun toInt(): kotlin.Int
    @kotlin.internal.InlineOnly public final inline fun toLong(): kotlin.Long
    @kotlin.internal.InlineOnly public final inline fun toShort(): kotlin.Short
    public open override /*1*/ fun toString(): kotlin.String
    @kotlin.internal.InlineOnly public final inline fun toUByte(): kotlin.UByte
    @kotlin.internal.InlineOnly public final inline fun toUInt(): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline fun toULong(): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline fun toUShort(): kotlin.UShort
    @kotlin.internal.InlineOnly public final inline infix fun xor(/*0*/ other: kotlin.UInt): kotlin.UInt

    public companion object Companion {
        public const final val MAX_VALUE: kotlin.UInt = -1.toUInt()
            public final fun <get-MAX_VALUE>(): kotlin.UInt
        public const final val MIN_VALUE: kotlin.UInt = 0.toUInt()
            public final fun <get-MIN_VALUE>(): kotlin.UInt
        public const final val SIZE_BITS: kotlin.Int = 32
            public final fun <get-SIZE_BITS>(): kotlin.Int
        public const final val SIZE_BYTES: kotlin.Int = 4
            public final fun <get-SIZE_BYTES>(): kotlin.Int
    }
}

@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public final inline class UIntArray : kotlin.collections.Collection<kotlin.UInt> {
    public constructor UIntArray(/*0*/ size: kotlin.Int)
    public open override /*1*/ val size: kotlin.Int
        public open override /*1*/ fun <get-size>(): kotlin.Int
    public open override /*1*/ fun contains(/*0*/ element: kotlin.UInt): kotlin.Boolean
    public open override /*1*/ fun containsAll(/*0*/ elements: kotlin.collections.Collection<kotlin.UInt>): kotlin.Boolean
    public open override /*1*/ /*synthesized*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public final operator fun get(/*0*/ index: kotlin.Int): kotlin.UInt
    public open override /*1*/ /*synthesized*/ fun hashCode(): kotlin.Int
    public open override /*1*/ fun isEmpty(): kotlin.Boolean
    public open override /*1*/ fun iterator(): kotlin.collections.UIntIterator
    public final operator fun set(/*0*/ index: kotlin.Int, /*1*/ value: kotlin.UInt): kotlin.Unit
    public open override /*1*/ /*synthesized*/ fun toString(): kotlin.String
}

@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public final inline class ULong : kotlin.Comparable<kotlin.ULong> {
    @kotlin.internal.InlineOnly public final inline infix fun and(/*0*/ other: kotlin.ULong): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun compareTo(/*0*/ other: kotlin.UByte): kotlin.Int
    @kotlin.internal.InlineOnly public final inline operator fun compareTo(/*0*/ other: kotlin.UInt): kotlin.Int
    @kotlin.internal.InlineOnly public open override /*1*/ inline fun compareTo(/*0*/ other: kotlin.ULong): kotlin.Int
    @kotlin.internal.InlineOnly public final inline operator fun compareTo(/*0*/ other: kotlin.UShort): kotlin.Int
    @kotlin.internal.InlineOnly public final inline operator fun dec(): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun div(/*0*/ other: kotlin.UByte): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun div(/*0*/ other: kotlin.UInt): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun div(/*0*/ other: kotlin.ULong): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun div(/*0*/ other: kotlin.UShort): kotlin.ULong
    public open override /*1*/ /*synthesized*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public open override /*1*/ /*synthesized*/ fun hashCode(): kotlin.Int
    @kotlin.internal.InlineOnly public final inline operator fun inc(): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline fun inv(): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun minus(/*0*/ other: kotlin.UByte): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun minus(/*0*/ other: kotlin.UInt): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun minus(/*0*/ other: kotlin.ULong): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun minus(/*0*/ other: kotlin.UShort): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline infix fun or(/*0*/ other: kotlin.ULong): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun plus(/*0*/ other: kotlin.UByte): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun plus(/*0*/ other: kotlin.UInt): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun plus(/*0*/ other: kotlin.ULong): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun plus(/*0*/ other: kotlin.UShort): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun rangeTo(/*0*/ other: kotlin.ULong): kotlin.ranges.ULongRange
    @kotlin.internal.InlineOnly public final inline operator fun rem(/*0*/ other: kotlin.UByte): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun rem(/*0*/ other: kotlin.UInt): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun rem(/*0*/ other: kotlin.ULong): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun rem(/*0*/ other: kotlin.UShort): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline infix fun shl(/*0*/ bitCount: kotlin.Int): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline infix fun shr(/*0*/ bitCount: kotlin.Int): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun times(/*0*/ other: kotlin.UByte): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun times(/*0*/ other: kotlin.UInt): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun times(/*0*/ other: kotlin.ULong): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun times(/*0*/ other: kotlin.UShort): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline fun toByte(): kotlin.Byte
    @kotlin.internal.InlineOnly public final inline fun toDouble(): kotlin.Double
    @kotlin.internal.InlineOnly public final inline fun toFloat(): kotlin.Float
    @kotlin.internal.InlineOnly public final inline fun toInt(): kotlin.Int
    @kotlin.internal.InlineOnly public final inline fun toLong(): kotlin.Long
    @kotlin.internal.InlineOnly public final inline fun toShort(): kotlin.Short
    public open override /*1*/ fun toString(): kotlin.String
    @kotlin.internal.InlineOnly public final inline fun toUByte(): kotlin.UByte
    @kotlin.internal.InlineOnly public final inline fun toUInt(): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline fun toULong(): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline fun toUShort(): kotlin.UShort
    @kotlin.internal.InlineOnly public final inline infix fun xor(/*0*/ other: kotlin.ULong): kotlin.ULong

    public companion object Companion {
        public const final val MAX_VALUE: kotlin.ULong = -1.toULong()
            public final fun <get-MAX_VALUE>(): kotlin.ULong
        public const final val MIN_VALUE: kotlin.ULong = 0.toULong()
            public final fun <get-MIN_VALUE>(): kotlin.ULong
        public const final val SIZE_BITS: kotlin.Int = 64
            public final fun <get-SIZE_BITS>(): kotlin.Int
        public const final val SIZE_BYTES: kotlin.Int = 8
            public final fun <get-SIZE_BYTES>(): kotlin.Int
    }
}

@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public final inline class ULongArray : kotlin.collections.Collection<kotlin.ULong> {
    public constructor ULongArray(/*0*/ size: kotlin.Int)
    public open override /*1*/ val size: kotlin.Int
        public open override /*1*/ fun <get-size>(): kotlin.Int
    public open override /*1*/ fun contains(/*0*/ element: kotlin.ULong): kotlin.Boolean
    public open override /*1*/ fun containsAll(/*0*/ elements: kotlin.collections.Collection<kotlin.ULong>): kotlin.Boolean
    public open override /*1*/ /*synthesized*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public final operator fun get(/*0*/ index: kotlin.Int): kotlin.ULong
    public open override /*1*/ /*synthesized*/ fun hashCode(): kotlin.Int
    public open override /*1*/ fun isEmpty(): kotlin.Boolean
    public open override /*1*/ fun iterator(): kotlin.collections.ULongIterator
    public final operator fun set(/*0*/ index: kotlin.Int, /*1*/ value: kotlin.ULong): kotlin.Unit
    public open override /*1*/ /*synthesized*/ fun toString(): kotlin.String
}

@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public final inline class UShort : kotlin.Comparable<kotlin.UShort> {
    @kotlin.internal.InlineOnly public final inline infix fun and(/*0*/ other: kotlin.UShort): kotlin.UShort
    @kotlin.internal.InlineOnly public final inline operator fun compareTo(/*0*/ other: kotlin.UByte): kotlin.Int
    @kotlin.internal.InlineOnly public final inline operator fun compareTo(/*0*/ other: kotlin.UInt): kotlin.Int
    @kotlin.internal.InlineOnly public final inline operator fun compareTo(/*0*/ other: kotlin.ULong): kotlin.Int
    @kotlin.internal.InlineOnly public open override /*1*/ inline fun compareTo(/*0*/ other: kotlin.UShort): kotlin.Int
    @kotlin.internal.InlineOnly public final inline operator fun dec(): kotlin.UShort
    @kotlin.internal.InlineOnly public final inline operator fun div(/*0*/ other: kotlin.UByte): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun div(/*0*/ other: kotlin.UInt): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun div(/*0*/ other: kotlin.ULong): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun div(/*0*/ other: kotlin.UShort): kotlin.UInt
    public open override /*1*/ /*synthesized*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public open override /*1*/ /*synthesized*/ fun hashCode(): kotlin.Int
    @kotlin.internal.InlineOnly public final inline operator fun inc(): kotlin.UShort
    @kotlin.internal.InlineOnly public final inline fun inv(): kotlin.UShort
    @kotlin.internal.InlineOnly public final inline operator fun minus(/*0*/ other: kotlin.UByte): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun minus(/*0*/ other: kotlin.UInt): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun minus(/*0*/ other: kotlin.ULong): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun minus(/*0*/ other: kotlin.UShort): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline infix fun or(/*0*/ other: kotlin.UShort): kotlin.UShort
    @kotlin.internal.InlineOnly public final inline operator fun plus(/*0*/ other: kotlin.UByte): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun plus(/*0*/ other: kotlin.UInt): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun plus(/*0*/ other: kotlin.ULong): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun plus(/*0*/ other: kotlin.UShort): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun rangeTo(/*0*/ other: kotlin.UShort): kotlin.ranges.UIntRange
    @kotlin.internal.InlineOnly public final inline operator fun rem(/*0*/ other: kotlin.UByte): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun rem(/*0*/ other: kotlin.UInt): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun rem(/*0*/ other: kotlin.ULong): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun rem(/*0*/ other: kotlin.UShort): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun times(/*0*/ other: kotlin.UByte): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun times(/*0*/ other: kotlin.UInt): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline operator fun times(/*0*/ other: kotlin.ULong): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline operator fun times(/*0*/ other: kotlin.UShort): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline fun toByte(): kotlin.Byte
    @kotlin.internal.InlineOnly public final inline fun toDouble(): kotlin.Double
    @kotlin.internal.InlineOnly public final inline fun toFloat(): kotlin.Float
    @kotlin.internal.InlineOnly public final inline fun toInt(): kotlin.Int
    @kotlin.internal.InlineOnly public final inline fun toLong(): kotlin.Long
    @kotlin.internal.InlineOnly public final inline fun toShort(): kotlin.Short
    public open override /*1*/ fun toString(): kotlin.String
    @kotlin.internal.InlineOnly public final inline fun toUByte(): kotlin.UByte
    @kotlin.internal.InlineOnly public final inline fun toUInt(): kotlin.UInt
    @kotlin.internal.InlineOnly public final inline fun toULong(): kotlin.ULong
    @kotlin.internal.InlineOnly public final inline fun toUShort(): kotlin.UShort
    @kotlin.internal.InlineOnly public final inline infix fun xor(/*0*/ other: kotlin.UShort): kotlin.UShort

    public companion object Companion {
        public const final val MAX_VALUE: kotlin.UShort = -1.toUShort()
            public final fun <get-MAX_VALUE>(): kotlin.UShort
        public const final val MIN_VALUE: kotlin.UShort = 0.toUShort()
            public final fun <get-MIN_VALUE>(): kotlin.UShort
        public const final val SIZE_BITS: kotlin.Int = 16
            public final fun <get-SIZE_BITS>(): kotlin.Int
        public const final val SIZE_BYTES: kotlin.Int = 2
            public final fun <get-SIZE_BYTES>(): kotlin.Int
    }
}

@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public final inline class UShortArray : kotlin.collections.Collection<kotlin.UShort> {
    public constructor UShortArray(/*0*/ size: kotlin.Int)
    public open override /*1*/ val size: kotlin.Int
        public open override /*1*/ fun <get-size>(): kotlin.Int
    public open override /*1*/ fun contains(/*0*/ element: kotlin.UShort): kotlin.Boolean
    public open override /*1*/ fun containsAll(/*0*/ elements: kotlin.collections.Collection<kotlin.UShort>): kotlin.Boolean
    public open override /*1*/ /*synthesized*/ fun equals(/*0*/ other: kotlin.Any?): kotlin.Boolean
    public final operator fun get(/*0*/ index: kotlin.Int): kotlin.UShort
    public open override /*1*/ /*synthesized*/ fun hashCode(): kotlin.Int
    public open override /*1*/ fun isEmpty(): kotlin.Boolean
    public open override /*1*/ fun iterator(): kotlin.collections.UShortIterator
    public final operator fun set(/*0*/ index: kotlin.Int, /*1*/ value: kotlin.UShort): kotlin.Unit
    public open override /*1*/ /*synthesized*/ fun toString(): kotlin.String
}

public open class UninitializedPropertyAccessException : kotlin.RuntimeException {
    public constructor UninitializedPropertyAccessException()
    public constructor UninitializedPropertyAccessException(/*0*/ message: kotlin.String?)
    public constructor UninitializedPropertyAccessException(/*0*/ message: kotlin.String?, /*1*/ cause: kotlin.Throwable?)
    public constructor UninitializedPropertyAccessException(/*0*/ cause: kotlin.Throwable?)
}

public object Unit {
    public open override /*1*/ fun toString(): kotlin.String
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.TYPE}) @kotlin.annotation.Retention(value = AnnotationRetention.SOURCE) @kotlin.annotation.MustBeDocumented public final annotation class UnsafeVariance : kotlin.Annotation {
    /*primary*/ public constructor UnsafeVariance()
}

public open class UnsupportedOperationException : kotlin.RuntimeException {
    public constructor UnsupportedOperationException()
    public constructor UnsupportedOperationException(/*0*/ message: kotlin.String?)
    public constructor UnsupportedOperationException(/*0*/ message: kotlin.String?, /*1*/ cause: kotlin.Throwable?)
    public constructor UnsupportedOperationException(/*0*/ cause: kotlin.Throwable?)
}

@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.EXPRESSION, AnnotationTarget.FILE, AnnotationTarget.TYPEALIAS}) @kotlin.annotation.Retention(value = AnnotationRetention.SOURCE) @kotlin.SinceKotlin(version = "1.2") @kotlin.Deprecated(message = "Please use OptIn instead.", replaceWith = kotlin.ReplaceWith(expression = "OptIn(*markerClass)", imports = {"kotlin.OptIn"})) public final annotation class UseExperimental : kotlin.Annotation {
    /*primary*/ public constructor UseExperimental(/*0*/ vararg markerClass: kotlin.reflect.KClass<out kotlin.Annotation> /*kotlin.Array<out kotlin.reflect.KClass<out kotlin.Annotation>>*/)
    public final val markerClass: kotlin.Array<out kotlin.reflect.KClass<out kotlin.Annotation>>
        public final fun <get-markerClass>(): kotlin.Array<out kotlin.reflect.KClass<out kotlin.Annotation>>
}
@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Do not use Synchronized annotation in pure Kotlin/JS code") public typealias Synchronized = kotlin.jvm.Synchronized
@kotlin.Deprecated(level = DeprecationLevel.ERROR, message = "Do not use Volatile annotation in pure Kotlin/JS code") public typealias Volatile = kotlin.jvm.Volatile
