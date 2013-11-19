package demo
class TestT() {
open fun getT<T>() {
}
}
class U() {
open fun main() {
val t = TestT()
t.getT<String>()
t.getT<Int>()
t.getT()
}
}