// EXPECTED_REACHABLE_NODES: 489
/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/defaultValues/simpleDefaultMethod.1.kt
 */

// FILE: foo.kt
package foo

import test.*

fun testCompilation(): String {
    emptyFun()
    emptyFun("K")

    return "OK"
}

fun simple(): String {
    return simpleFun() + simpleFun("K")
}

fun box(): String {
    var result = testCompilation()
    if (result != "OK") return "fail1: ${result}"

    result = simple()
    if (result != "OK") return "fail2: ${result}"

    return "OK"
}


// FILE: test.kt
package test

inline fun emptyFun(arg: String = "O") {

}

inline fun simpleFun(arg: String = "O"): String {
    val r = arg;
    return r;
}
