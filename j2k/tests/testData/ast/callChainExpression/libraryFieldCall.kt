class Library {
    class object {
        val ourOut: java.io.PrintStream = 0
    }
}

class User {
    fun main() {
        Library.ourOut.print()
    }
}