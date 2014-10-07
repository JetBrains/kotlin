/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/defaultValues/defaultMethod.2.kt
 */

package test

inline fun <T> simpleFun(arg: String = "O", lambda: (String) -> T): T {
    return lambda(arg)
}

inline fun <T> simpleFunR(lambda: (String) -> T, arg: String = "O"): T {
    return lambda(arg)
}

