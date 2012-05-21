open class Library() {
class object {
val ourOut : java.io.PrintStream? = null
}
}
open class User() {
open fun main() : Unit {
Library.ourOut?.print()
}
}