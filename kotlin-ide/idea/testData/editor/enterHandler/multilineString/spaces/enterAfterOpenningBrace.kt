fun test = """
    {
        abc
        abc {<caret>
    }
""".trimIndent()
//-----
fun test = """
    {
        abc
        abc {
        <caret>
    }
""".trimIndent()