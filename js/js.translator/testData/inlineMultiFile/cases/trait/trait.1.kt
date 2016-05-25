/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/trait/trait.1.kt
 */

package foo

import test.*

// CHECK_CONTAINS_NO_CALLS: testClassObject_0

internal fun testFinalInline(): String {
    return Z().finalInline({"final"})
}

internal fun testFinalInline2(instance: InlineTrait): String {
    return instance.finalInline({"final2"})
}

internal fun testClassObject(): String {
    return InlineTrait.finalInline({"classobject"})
}

fun box(): String {
    if (testFinalInline() != "final") return "test1: ${testFinalInline()}"
    if (testFinalInline2(Z()) != "final2") return "test2: ${testFinalInline2(Z())}"
    if (testClassObject() != "classobject") return "test3: ${testClassObject()}"

    return "OK"
}