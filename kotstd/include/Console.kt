package kotlin

/*
* Library for console interaction
*/

external fun kotlinclib_print_int(message: Int)
external fun kotlinclib_print_long(message: Long)
external fun kotlinclib_print_byte(message: Byte)
external fun kotlinclib_print_short(message: Short)
external fun kotlinclib_print_char(message: Char)
external fun kotlinclib_print_boolean(message: Boolean)
external fun kotlinclib_print_float(message: Float)
external fun kotlinclib_print_double(message: Double)
external fun kotlinclib_print_string(message: String)
external fun kotlinclib_println_int(message: Int)
external fun kotlinclib_println_long(message: Long)
external fun kotlinclib_println_byte(message: Byte)
external fun kotlinclib_println_short(message: Short)
external fun kotlinclib_println_char(message: Char)
external fun kotlinclib_println_boolean(message: Boolean)
external fun kotlinclib_println_float(message: Float)
external fun kotlinclib_println_double(message: Double)
external fun kotlinclib_println_string(message: String)
external fun kotlinclib_println()

/** Prints the given message to the standard output stream. */
fun print(message: Int) {
    kotlinclib_print_int(message)
}

/** Prints the given message to the standard output stream. */
fun print(message: Long) {
    kotlinclib_print_long(message)
}

/** Prints the given message to the standard output stream. */
fun print(message: Byte) {
    kotlinclib_print_byte(message)
}

/** Prints the given message to the standard output stream. */
fun print(message: Short) {
    kotlinclib_print_short(message)
}

/** Prints the given message to the standard output stream. */
fun print(message: Char) {
    kotlinclib_print_char(message)
}

/** Prints the given message to the standard output stream. */
fun print(message: Boolean) {
    kotlinclib_print_boolean(message)
}

/** Prints the given message to the standard output stream. */
fun print(message: Float) {
    kotlinclib_print_float(message)
}

/** Prints the given message to the standard output stream. */
fun print(message: Double) {
    kotlinclib_print_double(message)
}

/** Prints the given message to the standard output stream. */
fun print(message: String) {
    kotlinclib_print_string(message)
}

/** Prints the given message and newline to the standard output stream. */
fun print(message: ByteArray) {
    message.print()
}

/** Prints the given message and newline to the standard output stream. */
fun print(message: BooleanArray) {
    message.print()
}

/** Prints the given message and newline to the standard output stream. */
fun print(message: IntArray) {
    message.print()
}

/** Prints the given message and newline to the standard output stream. */
fun print(message: LongArray) {
    message.print()
}

/** Prints the given message and newline to the standard output stream. */
fun print(message: ShortArray) {
    message.print()
}


/** Prints the given message and newline to the standard output stream. */
fun println(message: Int) {
    kotlinclib_println_int(message)
}

/** Prints the given message and newline to the standard output stream. */
fun println(message: Long) {
    kotlinclib_println_long(message)
}

/** Prints the given message and newline to the standard output stream. */
fun println(message: Byte) {
    kotlinclib_println_byte(message)
}

/** Prints the given message and newline to the standard output stream. */
fun println(message: Short) {
    kotlinclib_println_short(message)
}

/** Prints the given message and newline to the standard output stream. */
fun println(message: Char) {
    kotlinclib_println_char(message)
}

/** Prints the given message and newline to the standard output stream. */
fun println(message: Boolean) {
    kotlinclib_println_boolean(message)
}

/** Prints the given message and newline to the standard output stream. */
fun println(message: Float) {
    kotlinclib_println_float(message)
}

/** Prints the given message and newline to the standard output stream. */
fun println(message: Double) {
    kotlinclib_println_double(message)
}

/** Prints the given message and newline to the standard output stream. */
fun println(message: String) {
    kotlinclib_println_string(message)
}

/** Prints the given message and newline to the standard output stream. */
fun println(message: ByteArray) {
    message.println()
}

/** Prints the given message and newline to the standard output stream. */
fun println(message: BooleanArray) {
    message.println()
}

/** Prints the given message and newline to the standard output stream. */
fun println(message: IntArray) {
    message.println()
}

/** Prints the given message and newline to the standard output stream. */
fun println(message: LongArray) {
    message.println()
}

/** Prints the given message and newline to the standard output stream. */
fun println(message: ShortArray) {
    message.println()
}

/** Prints newline to the standard output stream. */
fun println() {
    kotlinclib_println()
}