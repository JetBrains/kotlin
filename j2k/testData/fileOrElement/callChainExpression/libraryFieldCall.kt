// ERROR: Property must be initialized or be abstract
internal object Library {
    val ourOut: java.io.PrintStream
}

internal class User {
    fun main() {
        Library.ourOut.print(1)
    }
}