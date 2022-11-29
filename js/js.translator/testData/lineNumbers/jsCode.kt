fun foo() {
    var x = "foo"
    println("before: $x")
    js("console.log('js' + x);")
    println("after: $x")
}

// DONT_TARGET_EXACT_BACKEND: JS_IR
// ^There is a better stepping test

// LINES(JS):      1 6 2 2 3 3 4 4 5 5
