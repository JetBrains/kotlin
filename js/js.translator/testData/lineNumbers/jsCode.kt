fun foo() {
    var x = "foo"
    println("before: $x")
    js("console.log('js' + x);")
    println("after: $x")
}

// LINES(JS):    1 6 2 2 3 3 4 4     5 5
// LINES(JS_IR):     2 2 3 3     2 2 5 5
// FIXME:                    ^^^^^^^
//                           js function call debug info is incorrect
