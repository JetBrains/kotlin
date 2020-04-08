// FLOW: IN

open class C {
    var other: C? = null

    fun foo(p: Int) {
        println(p + 1)
        other?.bar(p + 1)
    }

    fun bar(<caret>p: Int) {
        println(p + 2)
    }
}

class D : C() {
    var other: D? = null

    override fun foo(p: Int) {
        println(p + 3)
        other?.bar(p + 3)
    }

    override fun bar(p: Int) {
        println(p + 4)
    }
}

