package demo

open class Program() {
    class object {
        public open fun main(args: Array<String?>?) {
            System.out?.println("Halo!")
        }
    }
}
fun main(args: Array<String>) = Program.main(args as Array<String?>?)