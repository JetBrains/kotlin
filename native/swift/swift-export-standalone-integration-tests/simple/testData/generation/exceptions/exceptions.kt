// KIND: STANDALONE
// MODULE: main
// FILE: main.kt

class Object {
    @Throws(Throwable::class)
    constructor(arg: Int) { error("?")}

    @Throws(Throwable::class)
    constructor(arg: Double) { error("?")}

    @Throws(Throwable::class)
    constructor(arg: Boolean) { error("?")}

    @Throws(Throwable::class)
    constructor(arg: Char) { error("?")}

    @Throws(Throwable::class)
    constructor(arg: Any) { error("?")}

    @Throws(Throwable::class)
    constructor(arg: Any?) { error("?")}

    @Throws(Throwable::class)
    constructor(arg: Object) { error("?")}
}

@Throws(Throwable::class)
fun throwing_fun_int(): Int = error("?")

@Throws(Throwable::class)
fun throwing_fun_int(arg: Int): Int = error("?")

@Throws(Throwable::class)
fun throwing_fun_double(): Double = error("?")

@Throws(Throwable::class)
fun throwing_fun_double(arg: Double): Double = error("?")

@Throws(Throwable::class)
fun throwing_fun_boolean(): Boolean = error("?")

@Throws(Throwable::class)
fun throwing_fun_boolean(arg: Boolean): Boolean = error("?")

@Throws(Throwable::class)
fun throwing_fun_char(): Char = error("?")

@Throws(Throwable::class)
fun throwing_fun_char(arg: Char): Char = error("?")

@Throws(Throwable::class)
fun throwing_fun_any(): Any = error("?")

@Throws(Throwable::class)
fun throwing_fun_any(arg: Any): Any = error("?")

@Throws(Throwable::class)
fun throwing_fun_object(): Object = error("?")

@Throws(Throwable::class)
fun throwing_fun_object(arg: Any): Object = error("?")

@Throws(Throwable::class)
fun throwing_fun_void(): Unit = error("?")

@Throws(Throwable::class)
fun throwing_fun_never(): Nothing = error("?")

@Throws(Throwable::class)
fun throwing_fun_nullable(): Any? = error("?")

@Throws(Throwable::class)
fun throwing_fun_nullable(arg: Any?): Any? = error("?")
