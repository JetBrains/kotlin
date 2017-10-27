fun main(args: Array<String>) {
    data class TContainer(val a: Int, val b: Int, val c: Int)

    class A<T>(val a: Int, val b: T, val c: Int, d: Int) {
//        fun deconstruct() = TContainer(a, b, c)
        fun component1() = a

        fun component2() = a to b

        fun component3() = "$c"
    }


    val k = A(1, 2, 3, 4)
    when (k) {
        match a @ A(p, Pair(b, c) if (b > c), e) -> {
            println("$a $b $c")
        }
        match (_, p :Pair, _) -> {
//            println("$a $b $c")
        }
    }
}