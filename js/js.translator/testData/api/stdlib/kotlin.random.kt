package kotlin.random

@kotlin.SinceKotlin(version = "1.3") public fun Random(/*0*/ seed: kotlin.Int): kotlin.random.Random
@kotlin.SinceKotlin(version = "1.3") public fun Random(/*0*/ seed: kotlin.Long): kotlin.random.Random
@kotlin.SinceKotlin(version = "1.3") public fun kotlin.random.Random.nextInt(/*0*/ range: kotlin.ranges.IntRange): kotlin.Int
@kotlin.SinceKotlin(version = "1.3") public fun kotlin.random.Random.nextLong(/*0*/ range: kotlin.ranges.LongRange): kotlin.Long
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.random.Random.nextUBytes(/*0*/ size: kotlin.Int): kotlin.UByteArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.random.Random.nextUBytes(/*0*/ array: kotlin.UByteArray): kotlin.UByteArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.random.Random.nextUBytes(/*0*/ array: kotlin.UByteArray, /*1*/ fromIndex: kotlin.Int = ..., /*2*/ toIndex: kotlin.Int = ...): kotlin.UByteArray
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.random.Random.nextUInt(): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.random.Random.nextUInt(/*0*/ until: kotlin.UInt): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.random.Random.nextUInt(/*0*/ from: kotlin.UInt, /*1*/ until: kotlin.UInt): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.random.Random.nextUInt(/*0*/ range: kotlin.ranges.UIntRange): kotlin.UInt
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.random.Random.nextULong(): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.random.Random.nextULong(/*0*/ until: kotlin.ULong): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.random.Random.nextULong(/*0*/ from: kotlin.ULong, /*1*/ until: kotlin.ULong): kotlin.ULong
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalUnsignedTypes public fun kotlin.random.Random.nextULong(/*0*/ range: kotlin.ranges.ULongRange): kotlin.ULong

@kotlin.SinceKotlin(version = "1.3") public abstract class Random {
    /*primary*/ public constructor Random()
    public abstract fun nextBits(/*0*/ bitCount: kotlin.Int): kotlin.Int
    public open fun nextBoolean(): kotlin.Boolean
    public open fun nextBytes(/*0*/ array: kotlin.ByteArray): kotlin.ByteArray
    public open fun nextBytes(/*0*/ array: kotlin.ByteArray, /*1*/ fromIndex: kotlin.Int = ..., /*2*/ toIndex: kotlin.Int = ...): kotlin.ByteArray
    public open fun nextBytes(/*0*/ size: kotlin.Int): kotlin.ByteArray
    public open fun nextDouble(): kotlin.Double
    public open fun nextDouble(/*0*/ until: kotlin.Double): kotlin.Double
    public open fun nextDouble(/*0*/ from: kotlin.Double, /*1*/ until: kotlin.Double): kotlin.Double
    public open fun nextFloat(): kotlin.Float
    public open fun nextInt(): kotlin.Int
    public open fun nextInt(/*0*/ until: kotlin.Int): kotlin.Int
    public open fun nextInt(/*0*/ from: kotlin.Int, /*1*/ until: kotlin.Int): kotlin.Int
    public open fun nextLong(): kotlin.Long
    public open fun nextLong(/*0*/ until: kotlin.Long): kotlin.Long
    public open fun nextLong(/*0*/ from: kotlin.Long, /*1*/ until: kotlin.Long): kotlin.Long

    @kotlin.Deprecated(level = DeprecationLevel.HIDDEN, message = "Use Default companion object instead") public object Companion : kotlin.random.Random {
        public open override /*1*/ fun nextBits(/*0*/ bitCount: kotlin.Int): kotlin.Int
    }

    public companion object Default : kotlin.random.Random {
        @kotlin.Deprecated(level = DeprecationLevel.HIDDEN, message = "Use Default companion object instead") public final val Companion: kotlin.random.Random.Companion
            public final fun <get-Companion>(): kotlin.random.Random.Companion
        public open override /*1*/ fun nextBits(/*0*/ bitCount: kotlin.Int): kotlin.Int
        public open override /*1*/ fun nextBoolean(): kotlin.Boolean
        public open override /*1*/ fun nextBytes(/*0*/ array: kotlin.ByteArray): kotlin.ByteArray
        public open override /*1*/ fun nextBytes(/*0*/ array: kotlin.ByteArray, /*1*/ fromIndex: kotlin.Int = ..., /*2*/ toIndex: kotlin.Int = ...): kotlin.ByteArray
        public open override /*1*/ fun nextBytes(/*0*/ size: kotlin.Int): kotlin.ByteArray
        public open override /*1*/ fun nextDouble(): kotlin.Double
        public open override /*1*/ fun nextDouble(/*0*/ until: kotlin.Double): kotlin.Double
        public open override /*1*/ fun nextDouble(/*0*/ from: kotlin.Double, /*1*/ until: kotlin.Double): kotlin.Double
        public open override /*1*/ fun nextFloat(): kotlin.Float
        public open override /*1*/ fun nextInt(): kotlin.Int
        public open override /*1*/ fun nextInt(/*0*/ until: kotlin.Int): kotlin.Int
        public open override /*1*/ fun nextInt(/*0*/ from: kotlin.Int, /*1*/ until: kotlin.Int): kotlin.Int
        public open override /*1*/ fun nextLong(): kotlin.Long
        public open override /*1*/ fun nextLong(/*0*/ until: kotlin.Long): kotlin.Long
        public open override /*1*/ fun nextLong(/*0*/ from: kotlin.Long, /*1*/ until: kotlin.Long): kotlin.Long
    }
}