// EXPECTED_REACHABLE_NODES: 1293
/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/simple/classObject.1.kt
 */

package foo

inline fun inline(s: () -> String): String {
    return s()
}

class InlineAll {

    inline fun inline(s: () -> String): String {
        return s()
    }

    companion object {
        inline fun inline(s: () -> String): String {
            return s()
        }
    }
}

// CHECK_BREAKS_COUNT: function=testClassObjectCall count=0
// CHECK_LABELS_COUNT: function=testClassObjectCall name=$l$block count=0
fun testClassObjectCall(): String {
    return InlineAll.inline({"classobject"})
}

// CHECK_BREAKS_COUNT: function=testInstanceCall count=0
// CHECK_LABELS_COUNT: function=testInstanceCall name=$l$block count=0
fun testInstanceCall(): String {
    val inlineX = InlineAll()
    return inlineX.inline({"instance"})
}

// CHECK_BREAKS_COUNT: function=testPackageCall count=0
// CHECK_LABELS_COUNT: function=testPackageCall name=$l$block count=0
fun testPackageCall(): String {
    return inline({"package"})
}

fun box(): String {
    if (testClassObjectCall() != "classobject") return "test1: ${testClassObjectCall()}"
    if (testInstanceCall() != "instance") return "test2: ${testInstanceCall()}"
    if (testPackageCall() != "package") return "test3: ${testPackageCall()}"
    return "OK"
}