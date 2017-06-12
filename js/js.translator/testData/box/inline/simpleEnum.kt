// EXPECTED_REACHABLE_NODES: 521
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

fun test1(): String {
    return MyEnum.K.doSmth("O")
}

fun box(): String {
    val result = test1()
    if (result != "OK") return "fail1: ${result}"

    return "OK"
}