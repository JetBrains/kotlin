fun t() {
    val c = aaaaaaaaa<caret>()


    aaaaaaaaa()
}

// above function
fun aaaaaaaaa() { // after open brace
    // eol comment in body
    /* block comment in body
    * second line */
    // topTest
    println(4) // test
    // above return
    return // after return
} // after close brace