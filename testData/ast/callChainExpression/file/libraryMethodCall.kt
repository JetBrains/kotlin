namespace {
open class Library {
class object {
fun call() : Unit {
}
fun getString() : String? {
return ""
}
}
}
open class User {
fun main() : Unit {
Library.call()
Library.getString()?.isEmpty()
}
}
}