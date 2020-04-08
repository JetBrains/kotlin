class C {
    var fooBar = ""
}

var fooBar = ""

fun f(s: String, c: C){
    if (c.fooBar == <caret>)
}

// ORDER: fooBar, s
