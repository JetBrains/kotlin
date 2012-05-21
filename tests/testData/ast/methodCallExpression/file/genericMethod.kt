package demo
open class Map() {
open fun put<K, V>(k : K?, v : V?) : Unit {
}
}
open class U() {
open fun test() : Unit {
var m : Map? = Map()
m?.put<String?, Int>("10", 10)
}
}