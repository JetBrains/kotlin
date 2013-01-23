package kotlin

public fun CharacterSequence.reversed(): CharacterSequence {
    return CharacterSequence(end, start, -increment)
}

public fun ByteSequence.reversed(): ByteSequence {
    return ByteSequence(end, start, -increment)
}

public fun ShortSequence.reversed(): ShortSequence {
    return ShortSequence(end, start, -increment)
}

public fun IntSequence.reversed(): IntSequence {
    return IntSequence(end, start, -increment)
}

public fun FloatSequence.reversed(): FloatSequence {
    return FloatSequence(end, start, -increment)
}

public fun LongSequence.reversed(): LongSequence {
    return LongSequence(end, start, -increment)
}

public fun DoubleSequence.reversed(): DoubleSequence {
    return DoubleSequence(end, start, -increment)
}


public fun CharRange.reversed(): CharacterSequence {
    return CharacterSequence(end, start, -1)
}

public fun ByteRange.reversed(): ByteSequence {
    return ByteSequence(end, start, -1)
}

public fun ShortRange.reversed(): ShortSequence {
    return ShortSequence(end, start, -1)
}

public fun IntRange.reversed(): IntSequence {
    return IntSequence(end, start, -1)
}

public fun FloatRange.reversed(): FloatSequence {
    return FloatSequence(end, start, -1.0.toFloat())
}

public fun LongRange.reversed(): LongSequence {
    return LongSequence(end, start, -1.toLong())
}
    
public fun DoubleRange.reversed(): DoubleSequence {
    return DoubleSequence(end, start, -1.0)
}


public fun IntSequence.step(step: Int): IntSequence {
    checkStepIsPositive(step > 0, step)
    return IntSequence(start, end, if (increment > 0) step else -step)
}

public fun CharacterSequence.step(step: Int): CharacterSequence {
    checkStepIsPositive(step > 0, step)
    return CharacterSequence(start, end, if (increment > 0) step else -step)
}

public fun ByteSequence.step(step: Int): ByteSequence {
    checkStepIsPositive(step > 0, step)
    return ByteSequence(start, end, if (increment > 0) step else -step)
}

public fun ShortSequence.step(step: Int): ShortSequence {
    checkStepIsPositive(step > 0, step)
    return ShortSequence(start, end, if (increment > 0) step else -step)
}

public fun LongSequence.step(step: Long): LongSequence {
    checkStepIsPositive(step > 0, step)
    return LongSequence(start, end, if (increment > 0) step else -step)
}

public fun FloatSequence.step(step: Float): FloatSequence {
    checkStepIsPositive(step > 0, step)
    return FloatSequence(start, end, if (increment > 0) step else -step)
}

public fun DoubleSequence.step(step: Double): DoubleSequence {
    checkStepIsPositive(step > 0, step)
    return DoubleSequence(start, end, if (increment > 0) step else -step)
}


public fun IntRange.step(step: Int): IntSequence {
    checkStepIsPositive(step > 0, step)
    return IntSequence(start, end, step)
}

public fun CharRange.step(step: Int): CharacterSequence {
    checkStepIsPositive(step > 0, step)
    return CharacterSequence(start, end, step)
}

public fun ByteRange.step(step: Int): ByteSequence {
    checkStepIsPositive(step > 0, step)
    return ByteSequence(start, end, step)
}

public fun ShortRange.step(step: Int): ShortSequence {
    checkStepIsPositive(step > 0, step)
    return ShortSequence(start, end, step)
}

public fun LongRange.step(step: Long): LongSequence {
    checkStepIsPositive(step > 0, step)
    return LongSequence(start, end, step)
}

public fun FloatRange.step(step: Float): FloatSequence {
    checkStepIsPositive(step > 0, step)
    return FloatSequence(start, end, step)
}

public fun DoubleRange.step(step: Double): DoubleSequence {
    checkStepIsPositive(step > 0, step)
    return DoubleSequence(start, end, step)
}


private fun checkStepIsPositive(isPositive: Boolean, step: Number) {
    if (!isPositive) throw IllegalArgumentException("Step must be positive, was: $step")
}
