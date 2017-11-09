package demo

import java.util.ArrayList

class TestJava {

    fun f(result: Function1<String, Unit>) {
        result.invoke("a")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val x = ArrayList<String>()
            x.filter { o -> o == "a" }
            val lazy = lazy(LazyThreadSafetyMode.NONE) { "aaa" }
        }
    }
}
