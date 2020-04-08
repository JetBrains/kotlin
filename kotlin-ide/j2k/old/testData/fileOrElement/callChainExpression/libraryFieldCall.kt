internal object Library {
    val ourOut: java.io.PrintStream? = null
}

internal class User {
    fun main() {
        Library.ourOut!!.print(1)
    }
}