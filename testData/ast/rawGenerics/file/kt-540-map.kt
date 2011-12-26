package demo
import java.util.HashMap
open class Test() {
open fun main() : Unit {
var commonMap : HashMap<String?, Int?>? = HashMap<String?, Int?>()
var rawMap : HashMap<*, *>? = HashMap<String?, Int?>()
var superRawMap : HashMap<*, *>? = HashMap()
}
}