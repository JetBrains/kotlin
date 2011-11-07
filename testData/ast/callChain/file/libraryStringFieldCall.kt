namespace {
open class Library {
public val myString : String?
}
open class User {
fun main() : Unit {
Library.myString?.isEmpty()
}
}
}