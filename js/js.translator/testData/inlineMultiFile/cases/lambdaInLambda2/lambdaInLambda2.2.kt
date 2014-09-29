/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/lambdaTransformation/lambdaInLambda2.2.kt
 */

package test

inline fun <R> mfun(f: () -> R) {
    f()
}

fun concat(suffix: String, l: (s: String) -> Unit)  {
    l(suffix)
}