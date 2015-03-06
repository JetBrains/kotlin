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

    default object {
        inline fun inline(s: () -> String): String {
            return s()
        }
    }
}

fun testClassObjectCall(): String {
    return InlineAll.inline({"classobject"})
}

fun testInstanceCall(): String {
    val inlineX = InlineAll()
    return inlineX.inline({"instance"})
}

fun testPackageCall(): String {
    return inline({"package"})
}

fun box(): String {
    if (testClassObjectCall() != "classobject") return "test1: ${testClassObjectCall()}"
    if (testInstanceCall() != "instance") return "test2: ${testInstanceCall()}"
    if (testPackageCall() != "package") return "test3: ${testPackageCall()}"
    return "OK"
}