// ERROR: The reference cannot be converted to a lambda

import kotlin.reflect.KFunction

fun te<caret>st() = "test"
fun takeKFunction(par: KFunction<String>): String = par.name
fun check() {
    takeKFunction(::test)
}