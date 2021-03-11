import AAA as A
public class X(bar: String? = A.BAR): A() {
    var next: A? = A()
    val myBar: String? = A.BAR

    init {
        A.BAR = ""
        AAA.foos()
    }

    fun foo(a: A) {
        val aa: AAA = a
        aa.bar = ""
    }

    fun getNext(): A? {
        return next
    }

    public override fun foo() {
        super<A>.foo()
    }

    companion object: AAA() {

    }
}

object O: A() {

}

fun X.bar(a: A = A()) {

}

fun Any.toA(): A? {
    return if (this is A) this as A else null
}