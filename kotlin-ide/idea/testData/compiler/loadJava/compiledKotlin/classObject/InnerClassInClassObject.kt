//ALLOW_AST_ACCESS
package test

class TestFirst {
  companion object {
    fun testing(a: InnerClass) = 45
    fun testing(a: NotInnerClass) = 45
  }

  inner class InnerClass
  inner class NotInnerClass
}
