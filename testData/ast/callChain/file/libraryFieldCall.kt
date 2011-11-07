namespace {
open class Library {
class object {
val ourOut : PrintStream?
}
}
open class User {
fun main() : Unit {
Library.ourOut?.print()
}
}
}