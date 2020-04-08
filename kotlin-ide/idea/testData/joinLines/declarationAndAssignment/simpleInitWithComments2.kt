fun foo(n: Int) {
    // foo3
    val /* foo */ x: String // foo2<caret>
    x = /* bar2 */ "" // bar3
    /* baz */
}