import java.io.PrintStream

internal object Library {
    val ourOut: PrintStream? = null
}

internal class User {
    fun main() {

        Library.ourOut!!.print(1)
    }
}