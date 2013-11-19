class Library() {
class object {
val ourOut : java.io.PrintStream = 0
}
}
class User() {
open fun main() {
Library.ourOut.print()
}
}