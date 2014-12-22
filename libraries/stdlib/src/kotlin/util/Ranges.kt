package kotlin

public class ComparableRange<T: Comparable<T>> (
        override val start: T,
        override val end: T
): Range<T> {
    override fun contains(item: T): Boolean {
        return start <= item && item <= end
    }

    override fun equals(other: Any?): Boolean {
        return other is ComparableRange<*> && (isEmpty() && other.isEmpty() ||
                start == other.start && end == other.end)
    }

    override fun hashCode(): Int {
        return if (isEmpty()) -1 else 31 * start.hashCode() + end.hashCode()
    }
}

public fun <T: Comparable<T>> T.rangeTo(that: T): ComparableRange<T> {
    return ComparableRange(this, that)
}



public fun CharProgression.reversed(): CharProgression {
    return CharProgression(end, start, -increment)
}

public fun ByteProgression.reversed(): ByteProgression {
    return ByteProgression(end, start, -increment)
}

public fun ShortProgression.reversed(): ShortProgression {
    return ShortProgression(end, start, -increment)
}

public fun IntProgression.reversed(): IntProgression {
    return IntProgression(end, start, -increment)
}

public fun FloatProgression.reversed(): FloatProgression {
    return FloatProgression(end, start, -increment)
}

public fun LongProgression.reversed(): LongProgression {
    return LongProgression(end, start, -increment)
}

public fun DoubleProgression.reversed(): DoubleProgression {
    return DoubleProgression(end, start, -increment)
}


public fun CharRange.reversed(): CharProgression {
    return CharProgression(end, start, -1)
}

public fun ByteRange.reversed(): ByteProgression {
    return ByteProgression(end, start, -1)
}

public fun ShortRange.reversed(): ShortProgression {
    return ShortProgression(end, start, -1)
}

public fun IntRange.reversed(): IntProgression {
    return IntProgression(end, start, -1)
}

public fun FloatRange.reversed(): FloatProgression {
    return FloatProgression(end, start, -1.0.toFloat())
}

public fun LongRange.reversed(): LongProgression {
    return LongProgression(end, start, -1.toLong())
}
    
public fun DoubleRange.reversed(): DoubleProgression {
    return DoubleProgression(end, start, -1.0)
}


public fun IntProgression.step(step: Int): IntProgression {
    checkStepIsPositive(step > 0, step)
    return IntProgression(start, end, if (increment > 0) step else -step)
}

public fun CharProgression.step(step: Int): CharProgression {
    checkStepIsPositive(step > 0, step)
    return CharProgression(start, end, if (increment > 0) step else -step)
}

public fun ByteProgression.step(step: Int): ByteProgression {
    checkStepIsPositive(step > 0, step)
    return ByteProgression(start, end, if (increment > 0) step else -step)
}

public fun ShortProgression.step(step: Int): ShortProgression {
    checkStepIsPositive(step > 0, step)
    return ShortProgression(start, end, if (increment > 0) step else -step)
}

public fun LongProgression.step(step: Long): LongProgression {
    checkStepIsPositive(step > 0, step)
    return LongProgression(start, end, if (increment > 0) step else -step)
}

public fun FloatProgression.step(step: Float): FloatProgression {
    checkStepIsPositive(step > 0, step)
    return FloatProgression(start, end, if (increment > 0) step else -step)
}

public fun DoubleProgression.step(step: Double): DoubleProgression {
    checkStepIsPositive(step > 0, step)
    return DoubleProgression(start, end, if (increment > 0) step else -step)
}


public fun IntRange.step(step: Int): IntProgression {
    checkStepIsPositive(step > 0, step)
    return IntProgression(start, end, step)
}

public fun CharRange.step(step: Int): CharProgression {
    checkStepIsPositive(step > 0, step)
    return CharProgression(start, end, step)
}

public fun ByteRange.step(step: Int): ByteProgression {
    checkStepIsPositive(step > 0, step)
    return ByteProgression(start, end, step)
}

public fun ShortRange.step(step: Int): ShortProgression {
    checkStepIsPositive(step > 0, step)
    return ShortProgression(start, end, step)
}

public fun LongRange.step(step: Long): LongProgression {
    checkStepIsPositive(step > 0, step)
    return LongProgression(start, end, step)
}

public fun FloatRange.step(step: Float): FloatProgression {
    if (step.isNaN()) throw IllegalArgumentException("Step must not be NaN")
    checkStepIsPositive(step > 0, step)
    return FloatProgression(start, end, step)
}

public fun DoubleRange.step(step: Double): DoubleProgression {
    if (step.isNaN()) throw IllegalArgumentException("Step must not be NaN")
    checkStepIsPositive(step > 0, step)
    return DoubleProgression(start, end, step)
}


private fun checkStepIsPositive(isPositive: Boolean, step: Number) {
    if (!isPositive) throw IllegalArgumentException("Step must be positive, was: $step")
}
