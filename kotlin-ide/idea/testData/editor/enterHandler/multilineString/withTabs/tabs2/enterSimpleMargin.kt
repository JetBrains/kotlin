class A {
	val a = """<caret>"""
}
//-----
class A {
	val a = """
		<caret>
	""".trimIndent()
}