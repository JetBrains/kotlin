class A {
    fun test() {
        println(toInl<caret>ine)
    }

    companion object : B() {
        val toInline = 42
    }
}

class B {
    init {
      println("Hey!")
    }
}