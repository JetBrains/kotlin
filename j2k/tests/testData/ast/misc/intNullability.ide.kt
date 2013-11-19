package demo
import kotlin.compatibility.*
open class Test() {
open fun test() {
var i : Int? = 10
var j : Int? = 10
System.out?.println(i!! + j!!)
}
}