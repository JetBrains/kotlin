package toplevelObjectDeclarations
  open class Foo(<warning>y</warning> : Int) {
    open fun foo() : Int = 1
  }

  class T : <error>Foo</error> {}

  object A : <error>Foo</error> {
    val x : Int = 2

    fun test() : Int {
      return x + foo()
    }
  }

  object B : <error>A</error> {}

  val x = A.foo()

  val y = object : Foo(x) {
    init {
      x + 12
    }

    override fun foo() : Int = 1
  }

  val z = y.foo()