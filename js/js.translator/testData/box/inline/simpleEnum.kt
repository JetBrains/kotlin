// EXPECTED_REACHABLE_NODES: 1293
/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/simple/simpleEnum.1.kt
 */

package foo

enum class MyEnum {
    K;

    //TODO: KT-4693
    inline fun <T> doSmth(a: T) : String {
        return a.toString() + K.name
    }
}

// CHECK_BREAKS_COUNT: function=test1 count=0 TARGET_BACKENDS=JS_IR
// CHECK_LABELS_COUNT: function=test1 name=$l$block count=0 TARGET_BACKENDS=JS_IR
fun test1(): String {
    return MyEnum.K.doSmth("O")
}

fun box(): String {
    val result = test1()
    if (result != "OK") return "fail1: ${result}"

    return "OK"
}