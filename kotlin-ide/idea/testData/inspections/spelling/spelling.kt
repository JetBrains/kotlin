fun xyzzy(): String {
    // this one is about xyzzy
    val s = "xyzzy ${xyzzy()} \n good\tbad\n"
    /* xyzzy in a block comment */
    /** xyzzy in a documentation comment */
    fun bar() {}
    return """xyzzy in a triple quoted string"""
}
