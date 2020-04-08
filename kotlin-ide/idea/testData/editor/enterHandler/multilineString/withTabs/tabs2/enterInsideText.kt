object A {
	val a = """blah<caret> blah"""
}
//-----
object A {
	val a = """blah
		| <caret>blah""".trimMargin()
}