package demo
open class TestT() {
open fun getT<T>() {
}
}
open class U() {
open fun main() {
val t = TestT()
t.getT<String>()
t.getT<Int>()
t.getT()
}
}