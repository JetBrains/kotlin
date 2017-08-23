class Test {

    private val isA = true

    private val isB = true

    private val c = C()

    @JvmOverloads
    fun foo(isA: Boolean = this.isA, isB: Boolean = this.isB, isC: Boolean = c.isC, isD: Boolean = D().isD) {
        println("isA=" + isA + ",isB=" + isB + "isC=" + isC + "isD=" + isD)
    }


    class C {
        var isC = true
    }

    class D {
        var isD = true
    }

}