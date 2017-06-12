// EXPECTED_REACHABLE_NODES: 495
// Order of evaluation differs for JVM and Javascript backend
// http://youtrack.jetbrains.com/issue/KT-5254

package foo

var s = ""

fun a():String {
    s += "A"
    return ""
}

fun b():String {
    s += "B"
    return ""
}

fun c():String {
    s += "C"
    return ""
}


fun box(): String {
    var res = (if(true) {a()} else "") + b() + (if (true) {c()} else "")
    assertEquals("ABC", s)

    return "OK"
}