namespace {
open class Library {
class object {
val ourOut : PrintStream?
}
}
open class User {
open fun main() : Unit {
Library.ourOut?.print()
}
}
}