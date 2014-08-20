// This test was adapted from compiler/testData/codegen/boxWithStdlib/callableReference/function/.
package foo

fun box(): String {
    if (true.(Boolean::not)() != false) return "Fail 1"
    if (false.(Boolean::not)() != true) return "Fail 2"
    return "OK"
}
