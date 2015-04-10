package foo

// CHECK_NOT_CALLED: buzz

private var LOG = ""

fun log(string: String) {
    LOG += "$string;"
}

fun pullLog(): String {
    val string = LOG
    LOG = ""
    return string
}

fun fizz<T>(x: T): T {
    log("fizz($x)")
    return x
}

inline
fun buzz<T>(x: T): T {
    log("buzz($x)")
    return x
}