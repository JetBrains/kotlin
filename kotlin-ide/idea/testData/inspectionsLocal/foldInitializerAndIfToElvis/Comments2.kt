fun foo(p: List<String?>): Int {
    val v = p[0]
    <caret>if (v == null) { // v is null
        // we should do something with it
        return -1 // let's return -1
    } // end of if
    return v.length
}