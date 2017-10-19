fun main(args: Array<String>) {
    //    when (5) {
//        match String -> {}
// //       match :String -> {}
//        match x -> {}
//    }
    data class TContainer(val a: Int, val b: Int, val c: Int)

    class A(val a: Int, val b: Int, val c: Int, d: Int) {
//        fun deconstruct() = TContainer(a, b, c)
        fun component1() = a

        fun component2() = a to b

        fun component3() = "$c"
    }


    val a = A(1, 2, 3, 4)
    when (a) {
        match e @ A(a, (b, c) if (b > c), 3) -> {
            println("$a $b $c")
        }
    }
}