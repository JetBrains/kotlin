//ALLOW_AST_ACCESS
package test

class A {
  companion object {
    val some = { 1 }()
  }

  val other = some
}