class Test {
    class B {
        class C

        fun c(): C {
            return C()
        }
    }

    fun a() {
        val b = B()
        println(b.toString() + "")
        val a = 1.toString() + "0"
        println(b.c().toString() + "")
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            val p = Test().toString() + "123"
        }
    }
}