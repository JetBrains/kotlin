namespace {
open class Library() {
public val myString : String?
}
open class User() {
open fun main() : Unit {
Library.myString?.isEmpty()
}
}
}