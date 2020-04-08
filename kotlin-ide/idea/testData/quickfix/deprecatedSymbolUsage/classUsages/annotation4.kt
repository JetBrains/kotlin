// "Replace with 'test.Bar(p, "")'" "true"

package test

@Deprecated("Replace with bar", ReplaceWith("test.Bar(p, \"\")"))
annotation class Foo(val p: Int)

annotation class Bar(val p: Int, val s: String)

@Foo<caret>(1) class C {}