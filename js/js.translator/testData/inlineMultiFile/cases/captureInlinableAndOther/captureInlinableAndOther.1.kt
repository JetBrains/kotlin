/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/capture/captureInlinableAndOther.1.kt
 */

package foo

import test.*

fun box(): String {
    val result = doWork({11})
    if (result != 11) return "test1: ${result}"

    val result2 = doWork({12; result+1})
    if (result2 != 12) return "test2: ${result2}"

    return "OK"
}