class A {
    fun test() {
        toInl<caret>ine()
    }

    companion object : B() {
        fun toInline() = Unit
    }
}

class B {
    init {
      println("Hey!")
    }
}