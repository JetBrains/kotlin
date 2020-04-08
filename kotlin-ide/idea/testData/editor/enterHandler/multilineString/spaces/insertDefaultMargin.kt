class A {
  val m = '#'
  val a = """blah blah<caret>""".trimMargin(m)
}
//-----
class A {
  val m = '#'
  val a = """blah blah
      |<caret>
  """.trimMargin(m)
}