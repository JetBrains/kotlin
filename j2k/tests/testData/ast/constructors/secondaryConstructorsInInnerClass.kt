class Outer {

    private fun Inner1(a: Int): Inner1 {
        return Inner1()
    }

    private fun Inner1(c: Char): Inner1 {
        return Inner1()
    }

    private fun Inner1(b: Boolean): Inner1 {
        return Inner1()
    }

    private inner class Inner1


    protected fun Inner2(a: Int): Inner2 {
        return Inner2()
    }

    protected fun Inner2(c: Char): Inner2 {
        return Inner2()
    }

    private fun Inner2(b: Boolean): Inner2 {
        return Inner2()
    }

    protected inner class Inner2


    fun Inner3(a: Int): Inner3 {
        return Inner3()
    }

    fun Inner3(c: Char): Inner3 {
        return Inner3()
    }

    private fun Inner3(b: Boolean): Inner3 {
        return Inner3()
    }

    inner class Inner3


    public fun Inner4(a: Int): Inner4 {
        return Inner4()
    }

    public fun Inner4(c: Char): Inner4 {
        return Inner4()
    }

    private fun Inner4(b: Boolean): Inner4 {
        return Inner4()
    }

    public inner class Inner4

    fun foo() {
        val inner1 = Inner1(1)
        val inner2 = Inner2(2)
        val inner3 = Inner3(3)
        val inner4 = Inner4(4)
    }
}