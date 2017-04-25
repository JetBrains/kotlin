// EXPECTED_REACHABLE_NODES: 498
// http://youtrack.jetbrains.com/issue/KT-4225
// Compiler to JavaScript produces semantically wrong code

package  foo

var i = 0
var j = 0

var global: String = ""

fun incI(){
    i++
}

fun incJ(a: Any){
    j++
}

fun foo(f: () -> Unit) = f

fun box(): String {
    val bar = 1

    val f = foo {
        incI()
        incJ(if (bar == 2) "A" else "B")
    }

    global +="i = $i, j = $j"
    f()
    global +=" : i = $i, j = $j"
    assertEquals("i = 0, j = 0 : i = 1, j = 1", global)

    return "OK"
}