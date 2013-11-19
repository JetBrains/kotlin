package demo
class TestT() {
fun getT<T>() {
}
}
class U() {
fun main() {
val t = TestT()
t.getT<String>()
t.getT<Int>()
t.getT()
}
}