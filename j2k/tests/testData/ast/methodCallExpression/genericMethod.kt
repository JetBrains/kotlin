package demo
open class Map() {
open fun put<K, V>(k : K?, v : V?) {
}
}
open class U() {
open fun test() {
var m : Map? = Map()
m?.put<String?, Int>("10", 10)
}
}