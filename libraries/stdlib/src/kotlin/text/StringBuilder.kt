@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("StringsKt")

package kotlin

/**
 * Builds newly created StringBuilder using provided body.
 */
public inline fun StringBuilder(body: StringBuilder.() -> Unit): StringBuilder {
    val sb = StringBuilder()
    sb.body()
    return sb
}

/**
 * Appends all arguments to the given Appendable.
 */
public fun <T : Appendable> T.append(vararg value: CharSequence?): T {
    for (item in value)
        append(item)
    return this
}

/**
 * Appends all arguments to the given StringBuilder.
 */
public fun StringBuilder.append(vararg value: String?): StringBuilder {
    for (item in value)
        append(item)
    return this
}

/**
 * Appends all arguments to the given StringBuilder.
 */
public fun StringBuilder.append(vararg value: Any?): StringBuilder {
    for (item in value)
        append(item)
    return this
}
