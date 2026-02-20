// WITH_STDLIB

fun foo(f: suspend () -> Unit) {
}

fun box(): String {
    foo {
        println("aaa")
        js("debugger;")
        println("bbb")
    }
    return "OK"
}