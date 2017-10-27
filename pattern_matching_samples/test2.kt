fun main(args: Array<String>) {
    data class TContainer(val a: Int, val b: Int, val c: Int)

    class A(val a: Int, val b: Int, val c: Int, d: Int) {
//        fun deconstruct() = TContainer(a, b, c)
        fun component1() = a

        fun component2() = a to b

        fun component3() = "$c"
    }


    val k = A(1, 2, 3, 4)
    when (k) {
        match e @ A(a, (b, c) if (b > c), a) -> {
            println("$a $b $c")
        }
    }
}