fun foo() {
    var x = "foo"
    println("before: $x")
    js("console.log('js' + x);")
    println("after: $x")
}

// LINES: 1 6 2 2 3 3 4 4 5 5