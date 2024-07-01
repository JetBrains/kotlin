package text

import base.toIntUnsigned
import kotlin.math.min

@DslMarker
annotation class PrettyDslMarker

@PrettyDslMarker
class Pretty private constructor(appendable: Appendable, private val firstCharPrefix: String = "") {
    companion object {
        fun with(appendable: Appendable) = Pretty(appendable)
    }

    private var isFirstChar: Boolean = true

    private val appendable = object : CharAppendable {
        override fun append(char: Char): Appendable = also {
            if (isFirstChar) {
                appendable.append(firstCharPrefix)
                isFirstChar = false
            }
            appendable.append(char)
        }
    }

    fun struct(name: String, fn: Pretty.() -> Unit) {
        appendable.append(name)
        appendable.appendIndented { apply { Pretty(this, "\n").fn() } }
        isFirstChar = true
    }

    fun field(name: String, fn: Pretty.() -> Unit) {
        appendable.append(name)
        Pretty(appendable, ": ").fn()
        isFirstChar = true
    }

    fun item(fn: Appendable.() -> Unit) {
        Pretty(appendable, "").appendable.fn()
        isFirstChar = true
    }
}

fun Appendable.appendPretty(fn: Pretty.() -> Unit): Appendable = apply {
    Pretty.with(this).fn()
}

fun prettyString(fn: Pretty.() -> Unit) = run {
    StringBuilder().apply { appendPretty { fn() } }.toString()
}

fun prettyPrint(fn: Pretty.() -> Unit) {
    printAppendable { appendPretty(fn) }
}

fun prettyPrintln(fn: Pretty.() -> Unit) {
    prettyPrint(fn)
    println()
}

fun Pretty.name(enum: Enum<*>) = item {
    append(enum.name)
}

fun Pretty.boolean(boolean: Boolean) = item {
    append(if (boolean) "true" else "false")
}

fun Pretty.decimal(int: Int) = item {
    append(int.toString())
}

fun Pretty.decimal(long: Long) = item {
    append(long.toString())
}

fun Pretty.hexadecimal(int: Int) = item {
    append("0x${int.toUInt().toString(16)}")
}

fun Pretty.hexadecimal(long: Long) = item {
    append("0x${long.toULong().toString(16)}")
}

fun Pretty.binary(byteArray: ByteArray) {
    for (segment in byteArray.indices step 16) {
        item {
            appendFixedSize(16 * 3 + 3) {
                for (index in segment..<min(segment + 16, byteArray.size)) {
                    append(String.format("%02x", byteArray[index].toIntUnsigned()))
                    append(" ")
                }
            }
            appendNonISOControl {
                for (index in segment..<min(segment + 16, byteArray.size)) {
                    append(byteArray[index].toIntUnsigned().toChar())
                }
            }
        }
    }
}
