/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/lambdaTransformation/lambdaInLambdaNoInline.2.kt
 */

package test

fun concat(suffix: String, l: (s: String) -> Unit)  {
    l(suffix)
}

fun <T> noInlineFun(arg: T, f: (T) -> Unit) {
    f(arg)
}

inline fun doSmth(a: String): String {
    return a.toString()
}
