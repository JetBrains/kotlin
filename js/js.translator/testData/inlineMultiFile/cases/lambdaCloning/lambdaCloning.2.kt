/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/lambdaTransformation/lambdaCloning.2.kt
 */

package test

inline fun <T> doSmth(a: T) : String {
    return {a.toString()}()
}

inline fun <T> doSmth2(a: T) : String {
    return {{a.toString()}()}()
}