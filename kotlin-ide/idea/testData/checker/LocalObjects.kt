// FIR_COMPARISON
  object A {
    val x : Int = 0
  }

  open class Foo {
    fun foo() : Int = 1
  }

  fun test() {
    A.x
    val b = object : Foo() {
    }
    b.foo()

    <error>object B</error> {
      fun foo() {}
    }
    B.foo()
  }

  val bb = <error>B</error>.<error>foo</error>()
