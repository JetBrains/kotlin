class Test {
    private var x = 1
    private val a = true
    private val b = true
    private fun c(): Boolean {
        return true
    }

    private val isD: Boolean
        private get() = true

    private val e = E()

    class E {
        var ee = true
    }

    class F {
        var f = true
    }

    private val g: Int
        private get() = 1

    @JvmOverloads
    fun foo(a: Boolean = this.a, b: Boolean = this.b, c: Boolean = c(), isD: Boolean = this.isD, e: Boolean = this.e.ee, f: Boolean = F().f, g: Int = this.g) {
    }

    @JvmOverloads
    fun bar(a: Boolean = this.a, e: Boolean = this.a, f: Boolean = b) {
    }

    @JvmOverloads
    fun baz(a: Boolean = !this.a, x: Int = ++this.x, y: Int = this.x++, z: Int = this.x + this.x + 1) {
    }
}