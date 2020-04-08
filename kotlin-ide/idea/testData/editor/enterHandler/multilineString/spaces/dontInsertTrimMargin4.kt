val a = """blah blah blah<caret>""".replace(" ", "")?.replace("b", "p").trimIndent().length
//-----
val a = """blah blah blah
    <caret>
""".replace(" ", "")?.replace("b", "p").trimIndent().length