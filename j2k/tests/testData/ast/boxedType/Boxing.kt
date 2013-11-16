import java.util.ArrayList
open class Boxing() {
open fun test() : Unit {
var i : Int? = 0
var n : Number? = 0.0.toFloat()
i = 1
var j : Int = i!!
var k : Int? = i!! + 2
i = null
j = i!!
}
}