class A {
  val a = """blah blah<caret>""".trimMargin("#")
}
//-----
class A {
  val a = """blah blah
      #<caret>
  """.trimMargin("#")
}