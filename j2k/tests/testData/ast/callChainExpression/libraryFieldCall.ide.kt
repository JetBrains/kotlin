open class Library() {
class object {
val ourOut : java.io.PrintStream = 0
}
}
open class User() {
open fun main() : Unit {
Library.ourOut.print()
}
}