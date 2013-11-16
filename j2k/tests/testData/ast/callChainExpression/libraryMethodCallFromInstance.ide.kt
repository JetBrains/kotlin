open class Library() {
open fun call() : Unit {
}
open fun getString() : String {
return ""
}
}
open class User() {
open fun main() : Unit {
val lib = Library()
lib.call()
lib.getString().isEmpty()
Library().call()
Library().getString().isEmpty()
}
}