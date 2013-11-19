package demo
class Map() {
open fun put<K, V>(k : K, v : V) {
}
}
class U() {
open fun test() {
val m = Map()
m.put<String, Int>("10", 10)
}
}