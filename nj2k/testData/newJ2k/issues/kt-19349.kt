package test.hierarchy

open class Base {
    open fun doStuff(s: String?) {}
}

class Derived : Base() {
    override fun doStuff(s: String?) {
        if (s == null) println("null") else println("not null: $s")
    }
}
