package demo
class Map() {
fun put<K, V>(k : K, v : V) {
}
}
class U() {
fun test() {
val m = Map()
m.put<String, Int>("10", 10)
}
}