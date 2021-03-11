// ERROR: The reference cannot be converted to a lambda

import kotlin.reflect.KFunction

fun takeKFunction(par: KFunction<String>): String = par.name
fun check() {
    takeKFunction(Test::returnStri<caret>ng)
}