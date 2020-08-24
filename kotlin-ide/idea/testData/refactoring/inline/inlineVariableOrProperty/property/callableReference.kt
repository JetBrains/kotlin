// ERROR: The reference cannot be converted to a lambda

import kotlin.reflect.KCallable

val pr<caret>op = "str"

fun takeKProperty(par: KCallable<String>): String = par.name
fun check() {
    takeKProperty(::prop)
}