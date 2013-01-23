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

