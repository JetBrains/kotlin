class Owner {
    inner class Inner

    val x = { <caret>o: Owner -> o.Inner() }
}