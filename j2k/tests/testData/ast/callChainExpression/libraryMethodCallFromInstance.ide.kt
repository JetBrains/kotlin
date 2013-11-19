class Library() {
open fun call() {
}
open fun getString() : String {
return ""
}
}
class User() {
open fun main() {
val lib = Library()
lib.call()
lib.getString().isEmpty()
Library().call()
Library().getString().isEmpty()
}
}