package kotlin


deprecated("Use joinToString() instead")
public fun <T> Array<out T>.makeString(separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): String {
    return joinToString(separator, prefix, postfix, limit, truncated)
}

deprecated("Use joinToString() instead")
public fun BooleanArray.makeString(separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): String {
    return joinToString(separator, prefix, postfix, limit, truncated)
}

deprecated("Use joinToString() instead")
public fun ByteArray.makeString(separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): String {
    return joinToString(separator, prefix, postfix, limit, truncated)
}

deprecated("Use joinToString() instead")
public fun CharArray.makeString(separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): String {
    return joinToString(separator, prefix, postfix, limit, truncated)
}

deprecated("Use joinToString() instead")
public fun DoubleArray.makeString(separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): String {
    return joinToString(separator, prefix, postfix, limit, truncated)
}

deprecated("Use joinToString() instead")
public fun FloatArray.makeString(separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): String {
    return joinToString(separator, prefix, postfix, limit, truncated)
}

deprecated("Use joinToString() instead")
public fun IntArray.makeString(separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): String {
    return joinToString(separator, prefix, postfix, limit, truncated)
}

deprecated("Use joinToString() instead")
public fun LongArray.makeString(separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): String {
    return joinToString(separator, prefix, postfix, limit, truncated)
}

deprecated("Use joinToString() instead")
public fun ShortArray.makeString(separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): String {
    return joinToString(separator, prefix, postfix, limit, truncated)
}

deprecated("Use joinToString() instead")
public fun <T> Iterable<T>.makeString(separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): String {
    return joinToString(separator, prefix, postfix, limit, truncated)
}

deprecated("Use joinToString() instead")
public fun <T> Sequence<T>.makeString(separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): String {
    return joinToString(separator, prefix, postfix, limit, truncated)
}


deprecated("Use joinTo() instead")
public fun <T> Array<out T>.appendString(buffer: Appendable, separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): Unit {
    joinTo(buffer, separator, prefix, postfix, limit, truncated)
}

deprecated("Use joinTo() instead")
public fun BooleanArray.appendString(buffer: Appendable, separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): Unit {
    joinTo(buffer, separator, prefix, postfix, limit, truncated)
}

deprecated("Use joinTo() instead")
public fun ByteArray.appendString(buffer: Appendable, separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): Unit {
    joinTo(buffer, separator, prefix, postfix, limit, truncated)
}

deprecated("Use joinTo() instead")
public fun CharArray.appendString(buffer: Appendable, separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): Unit {
    joinTo(buffer, separator, prefix, postfix, limit, truncated)
}

deprecated("Use joinTo() instead")
public fun DoubleArray.appendString(buffer: Appendable, separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): Unit {
    joinTo(buffer, separator, prefix, postfix, limit, truncated)
}

deprecated("Use joinTo() instead")
public fun FloatArray.appendString(buffer: Appendable, separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): Unit {
    joinTo(buffer, separator, prefix, postfix, limit, truncated)
}

deprecated("Use joinTo() instead")
public fun IntArray.appendString(buffer: Appendable, separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): Unit {
    joinTo(buffer, separator, prefix, postfix, limit, truncated)
}

deprecated("Use joinTo() instead")
public fun LongArray.appendString(buffer: Appendable, separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): Unit {
    joinTo(buffer, separator, prefix, postfix, limit, truncated)
}

deprecated("Use joinTo() instead")
public fun ShortArray.appendString(buffer: Appendable, separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): Unit {
    joinTo(buffer, separator, prefix, postfix, limit, truncated)
}

deprecated("Use joinTo() instead")
public fun <T> Iterable<T>.appendString(buffer: Appendable, separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): Unit {
    joinTo(buffer, separator, prefix, postfix, limit, truncated)
}

deprecated("Use joinTo() instead")
public fun <T> Sequence<T>.appendString(buffer: Appendable, separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): Unit {
    joinTo(buffer, separator, prefix, postfix, limit, truncated)
}
