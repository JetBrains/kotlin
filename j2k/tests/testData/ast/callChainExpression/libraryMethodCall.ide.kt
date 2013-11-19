class Library() {
class object {
open fun call() {
}
open fun getString() : String {
return ""
}
}
}
class User() {
open fun main() {
Library.call()
Library.getString().isEmpty()
}
}