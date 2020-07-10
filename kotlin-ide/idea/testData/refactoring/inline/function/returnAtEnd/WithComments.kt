fun t() {
    val c = aaaaaaaaa<caret>()
}

// above function
fun aaaaaaaaa(): Int { // after open brace
// eol comment in body
    /* block comment in body
    * second line */


    // topTest
    println(4) // test
    // above return
    return 4 // after return
} // after close brace