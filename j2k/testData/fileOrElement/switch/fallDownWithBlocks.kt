public class C {
    default object {
        public fun main(args: Array<String>) {
            when (args.size) {
                1 -> {
                    run {
                        val a = 1
                        System.out.print("1")
                    }
                    run {
                        val a = 2
                        System.out.print("2")
                    }
                }

                2 -> {
                    val a = 2
                    System.out.print("2")
                }
            }
        }
    }
}

fun main(args: Array<String>) = C.main(args)