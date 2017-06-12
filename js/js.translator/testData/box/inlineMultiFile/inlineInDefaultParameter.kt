// EXPECTED_REACHABLE_NODES: 488
/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/defaultValues/inlineInDefaultParameter.1.kt
 */

// FILE: foo.kt
package foo

import test.*

fun testCompilation(arg: String = getStringInline()): String {
    return arg
}

inline fun testCompilationInline(arg: String = getStringInline()): String {
    return arg
}

fun box(): String {
    var result = testCompilation()
    if (result != "OK") return "fail1: ${result}"

    result = testCompilation("OKOK")
    if (result != "OKOK") return "fail2: ${result}"


    result = testCompilationInline()
    if (result != "OK") return "fail3: ${result}"

    result = testCompilationInline("OKOK")
    if (result != "OKOK") return "fail4: ${result}"

    return "OK"
}


// FILE: test.kt
package test

inline fun getStringInline(): String {
    return "OK"
}