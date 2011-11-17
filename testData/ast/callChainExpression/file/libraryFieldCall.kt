open class Library() {
class object {
val ourOut : java.io.PrintStream?
}
}
open class User() {
open fun main() : Unit {
Library.ourOut?.print()
}
}