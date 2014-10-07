/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/defaultValues/simpleDefaultMethod.2.kt
 */

package test

inline fun emptyFun(arg: String = "O") {

}

inline fun simpleFun(arg: String = "O"): String {
    val r = arg;
    return r;
}

