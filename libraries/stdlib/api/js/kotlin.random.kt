@kotlin.SinceKotlin(version = "1.3")
public fun Random(seed: kotlin.Int): kotlin.random.Random

@kotlin.SinceKotlin(version = "1.3")
public fun Random(seed: kotlin.Long): kotlin.random.Random

@kotlin.SinceKotlin(version = "1.3")
public fun kotlin.random.Random.nextInt(range: kotlin.ranges.IntRange): kotlin.Int

@kotlin.SinceKotlin(version = "1.3")
public fun kotlin.random.Random.nextLong(range: kotlin.ranges.LongRange): kotlin.Long

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.random.Random.nextUBytes(size: kotlin.Int): kotlin.UByteArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.random.Random.nextUBytes(array: kotlin.UByteArray): kotlin.UByteArray

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalUnsignedTypes
public fun kotlin.random.Random.nextUBytes(array: kotlin.UByteArray, fromIndex: kotlin.Int = ..., toIndex: kotlin.Int = ...): kotlin.UByteArray

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.random.Random.nextUInt(): kotlin.UInt

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.random.Random.nextUInt(until: kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.random.Random.nextUInt(from: kotlin.UInt, until: kotlin.UInt): kotlin.UInt

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.random.Random.nextUInt(range: kotlin.ranges.UIntRange): kotlin.UInt

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.random.Random.nextULong(): kotlin.ULong

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.random.Random.nextULong(until: kotlin.ULong): kotlin.ULong

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.random.Random.nextULong(from: kotlin.ULong, until: kotlin.ULong): kotlin.ULong

@kotlin.SinceKotlin(version = "1.5")
@kotlin.WasExperimental(markerClass = {kotlin.ExperimentalUnsignedTypes::class})
public fun kotlin.random.Random.nextULong(range: kotlin.ranges.ULongRange): kotlin.ULong

@kotlin.SinceKotlin(version = "1.3")
public abstract class Random {
    public constructor Random()

    public abstract fun nextBits(bitCount: kotlin.Int): kotlin.Int

    public open fun nextBoolean(): kotlin.Boolean

    public open fun nextBytes(array: kotlin.ByteArray): kotlin.ByteArray

    public open fun nextBytes(array: kotlin.ByteArray, fromIndex: kotlin.Int = ..., toIndex: kotlin.Int = ...): kotlin.ByteArray

    public open fun nextBytes(size: kotlin.Int): kotlin.ByteArray

    public open fun nextDouble(): kotlin.Double

    public open fun nextDouble(until: kotlin.Double): kotlin.Double

    public open fun nextDouble(from: kotlin.Double, until: kotlin.Double): kotlin.Double

    public open fun nextFloat(): kotlin.Float

    public open fun nextInt(): kotlin.Int

    public open fun nextInt(until: kotlin.Int): kotlin.Int

    public open fun nextInt(from: kotlin.Int, until: kotlin.Int): kotlin.Int

    public open fun nextLong(): kotlin.Long

    public open fun nextLong(until: kotlin.Long): kotlin.Long

    public open fun nextLong(from: kotlin.Long, until: kotlin.Long): kotlin.Long

    public companion object of Random Default : kotlin.random.Random, kotlin.io.Serializable {
        public open override fun nextBits(bitCount: kotlin.Int): kotlin.Int

        public open override fun nextBoolean(): kotlin.Boolean

        public open override fun nextBytes(array: kotlin.ByteArray): kotlin.ByteArray

        public open override fun nextBytes(array: kotlin.ByteArray, fromIndex: kotlin.Int = ..., toIndex: kotlin.Int = ...): kotlin.ByteArray

        public open override fun nextBytes(size: kotlin.Int): kotlin.ByteArray

        public open override fun nextDouble(): kotlin.Double

        public open override fun nextDouble(until: kotlin.Double): kotlin.Double

        public open override fun nextDouble(from: kotlin.Double, until: kotlin.Double): kotlin.Double

        public open override fun nextFloat(): kotlin.Float

        public open override fun nextInt(): kotlin.Int

        public open override fun nextInt(until: kotlin.Int): kotlin.Int

        public open override fun nextInt(from: kotlin.Int, until: kotlin.Int): kotlin.Int

        public open override fun nextLong(): kotlin.Long

        public open override fun nextLong(until: kotlin.Long): kotlin.Long

        public open override fun nextLong(from: kotlin.Long, until: kotlin.Long): kotlin.Long
    }
}