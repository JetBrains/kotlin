package test

fun asBaseInterface(c: Class): BaseInterface? = try {
    c as BaseInterface
} catch (t: Throwable) {
    null
}
