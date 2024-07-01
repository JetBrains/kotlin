package text

fun appendableString(fn: Appendable.() -> Unit) = StringBuilder().apply { fn() }.toString()

/** Appendable with default char-sequence functions. */
interface CharAppendable : Appendable {
    override fun append(csq: CharSequence?) = apply {
        (csq ?: "null").forEach { append(it) }
    }

    override fun append(csq: CharSequence?, start: Int, end: Int) = apply {
        append(csq?.subSequence(start, end))
    }
}

fun printAppendable(fn: Appendable.() -> Unit) {
    object : CharAppendable {
        override fun append(csq: CharSequence?) = apply {
            print(csq)
        }

        override fun append(c: Char) = apply {
            print(c)
        }
    }.fn()
}

fun Appendable.appendIndented(fn: Appendable.() -> Unit) = also { appendable ->
    object : CharAppendable {
        var isIndentStart = false

        override fun append(char: Char) = apply {
            if (char == '\n') {
                isIndentStart = true
            } else if (isIndentStart) {
                appendable.append("  ")
                isIndentStart = false
            }
            appendable.append(char)
        }
    }.fn()
}

/** Appendable which pads with spaces or truncates to the given size. */
fun Appendable.appendFixedSize(size: Int, fn: Appendable.() -> Unit) = also { appendable ->
    object : CharAppendable {
        var remaining = size

        override fun append(char: Char) = apply {
            if (remaining > 0) {
                appendable.append(char)
                remaining--
            }
        }

        fun pad() {
            repeat(remaining) {
                appendable.append(' ')
            }
        }
    }.apply { fn() }.pad()
}

/** Appendable which appends ISO-control characters as dot '.' */
fun Appendable.appendNonISOControl(fn: Appendable.() -> Unit) = also { appendable ->
    object : CharAppendable {
        override fun append(char: Char) = apply {
            appendable.append(char.takeIf { !it.isISOControl() } ?: '.')
        }
    }.fn()
}

