open class Library() {
class object {
open fun call() : Unit {
}
open fun getString() : String? {
return ""
}
}
}
open class User() {
open fun main() : Unit {
Library.call()
Library.getString()?.isEmpty()
}
}