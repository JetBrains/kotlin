namespace {
open class Library {
fun call() : Unit {
}
fun getString() : String? {
return ""
}
}
open class User {
fun main() : Unit {
var lib : Library? = Library()
lib?.call()
lib?.getString()?.isEmpty()
Library().call()
Library().getString()?.isEmpty()
}
}
}