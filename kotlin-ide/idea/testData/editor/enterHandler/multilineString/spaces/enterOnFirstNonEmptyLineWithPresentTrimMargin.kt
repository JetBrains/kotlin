fun some() {
    val b = """abc<caret>
        """.trimMargin()
}
//-----
fun some() {
    val b = """abc
        |<caret>
        """.trimMargin()
}
