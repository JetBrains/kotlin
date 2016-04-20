@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("StringsKt")

package kotlin.text

private object SystemProperties {
    /** Line separator for current system. */
    @JvmField
    val LINE_SEPARATOR = System.getProperty("line.separator")!!
}

/** Appends a line separator to this Appendable. */
public fun Appendable.appendln(): Appendable = append(SystemProperties.LINE_SEPARATOR)

/** Appends value to the given Appendable and line separator after it. */
@kotlin.internal.InlineOnly
public inline fun Appendable.appendln(value: CharSequence?): Appendable = append(value).appendln()

/** Appends value to the given Appendable and line separator after it. */
@kotlin.internal.InlineOnly
public inline fun Appendable.appendln(value: Char): Appendable = append(value).appendln()

/** Appends a line separator to this StringBuilder. */
public fun StringBuilder.appendln(): StringBuilder = append(SystemProperties.LINE_SEPARATOR)

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: StringBuffer?): StringBuilder = append(value).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: CharSequence?): StringBuilder = append(value).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: String?): StringBuilder = append(value).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: Any?): StringBuilder = append(value).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: StringBuilder?): StringBuilder = append(value).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: CharArray): StringBuilder = append(value).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: Char): StringBuilder = append(value).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: Boolean): StringBuilder = append(value).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: Int): StringBuilder = append(value).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: Short): StringBuilder = append(value.toInt()).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: Byte): StringBuilder = append(value.toInt()).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: Long): StringBuilder = append(value).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: Float): StringBuilder = append(value).appendln()

/** Appends [value] to this [StringBuilder], followed by a line separator. */
@kotlin.internal.InlineOnly
public inline fun StringBuilder.appendln(value: Double): StringBuilder = append(value).appendln()
