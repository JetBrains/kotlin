package test

fun testJs() {
    // This is a JavaScript module, so `List.stream` should be unresolved
    getEmptyList().<error>stream</error>()
}
