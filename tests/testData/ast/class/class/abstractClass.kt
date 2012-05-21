abstract class A() {
abstract fun callme() : Unit
open fun callmetoo() : Unit {
print("This is a concrete method.")
}
}