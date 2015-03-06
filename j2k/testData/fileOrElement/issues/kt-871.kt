package demo

class Program {
    default object {
        public fun main(args: Array<String>) {
            System.out.println("Halo!")
        }
    }
}

fun main(args: Array<String>) = Program.main(args)