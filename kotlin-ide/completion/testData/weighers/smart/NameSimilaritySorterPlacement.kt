// test that name similarity sorting takes over declaration kind sorting
val xGlobalBar = ""
val xGlobalX = ""

fun f(fooBar: String){}

fun g() {
    val xLocalBar = ""
    val xLocalX = ""
    f(x<caret>)
}

// ORDER: xLocalBar, xGlobalBar, xLocalX, xGlobalX
