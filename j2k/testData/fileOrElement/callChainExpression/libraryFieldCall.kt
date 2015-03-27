// ERROR: Property must be initialized or be abstract
object Library {
    val ourOut: java.io.PrintStream
}

class User {
    fun main() {
        Library.ourOut.print(1)
    }
}