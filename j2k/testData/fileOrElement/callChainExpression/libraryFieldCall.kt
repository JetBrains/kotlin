// ERROR: Property must be initialized or be abstract
internal object Library {
    internal val ourOut: java.io.PrintStream
}

internal class User {
    internal fun main() {
        Library.ourOut.print(1)
    }
}