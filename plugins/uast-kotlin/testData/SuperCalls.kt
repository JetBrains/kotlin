open class A(val str: String) {

    constructor(i: Int) : this(i.toString())

    open fun foo(a: Long) {}

}

class B(param: String) : A(param)

class C : A {
    constructor(p: String) : super(p)

    constructor(i: Int) : super(i)

    override fun foo(a: Long) {
        super.foo(a)
    }
}

object O : A("text")

val anon = object : A("textForAnon") {
    fun bar() {
        cons(object : A("inner literal"))
    }

    object innerObject : A("inner object")

    inner class InnerClass : A("inner class")
}

fun cons(a: A) {}