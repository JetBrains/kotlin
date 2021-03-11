fun some() {
    val b = """
        class Test {
            fun some() {<caret>}
        }
    """.trimIndent()
}
//-----
fun some() {
    val b = """
        class Test {
            fun some() {
            <caret>
            }
        }
    """.trimIndent()
}