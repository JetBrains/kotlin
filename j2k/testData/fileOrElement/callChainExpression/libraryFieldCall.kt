// ERROR: Property must be initialized or be abstract
class Library {
    default object {
        val ourOut: java.io.PrintStream
    }
}

class User {
    fun main() {
        Library.ourOut.print(1)
    }
}