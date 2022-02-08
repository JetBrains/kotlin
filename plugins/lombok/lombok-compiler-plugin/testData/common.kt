fun <T> assertEquals(a: T, b: T) {
    if (a != b) throw RuntimeException("'$a' was not equal to '$b'")
}
