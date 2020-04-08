annotation class X(val s: String)

@X("")
fun foo<caret>(): String = ""