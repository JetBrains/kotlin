// This test was adapted from compiler/testData/codegen/boxWithStdlib/callableReference/function/.
package foo

fun run(funRef:() -> String): String {
    return funRef()
}

fun bar() = "OK"

fun box(): String {
    val x = ::bar

    var r = x()
    if (r != "OK") return r

    r = run(::bar)
    if (r != "OK") return r

    return "OK"
}
