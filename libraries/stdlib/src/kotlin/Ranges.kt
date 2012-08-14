package kotlin

public val CharRange.reversed: CharRange
    get() = CharRange(end, if (start < end) -size else size)

public val ByteRange.reversed: ByteRange
    get() = ByteRange(end, if (start < end) -size else size)

public val ShortRange.reversed: ShortRange
    get() = ShortRange(end, if (start < end) -size else size)

public val IntRange.reversed: IntRange
    get() = IntRange(end, if (start < end) -size else size)

public val FloatRange.reversed: FloatRange
    get() = FloatRange(end, if (start < end) -size else size)

public val LongRange.reversed: LongRange
    get() = LongRange(end, if (start < end) -size else size)

public val DoubleRange.reversed: DoubleRange
    get() = DoubleRange(end, if (start < end) -size else size)

