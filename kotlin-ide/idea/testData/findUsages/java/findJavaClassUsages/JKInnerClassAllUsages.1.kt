public class KOuter: Outer() {
    public inner class X(bar: String? = (this@KOuter as Outer).A().bar): Outer.A() {
        var next: Outer.A? = (this@KOuter as Outer).A()
        val myBar: String? = (this@KOuter as Outer).A().bar

        init {
            (this@KOuter as Outer).A().bar = ""
        }

        fun foo(a: Outer.A) {
            val aa: Outer.A = a
            aa.bar = ""
        }

        fun getNext(): Outer.A? {
            return next
        }

        public override fun foo() {
            super<Outer.A>.foo()
        }
    }
}

fun KOuter.X.bar(a: Outer.A = Outer().A()) {

}

fun Any.toA(): Outer.A? {
    return if (this is Outer.A) this as Outer.A else null
}