import java.util.HashMap
open class G<T : String?>(t : T?) {
}
public open class Java() {
open fun test() : Unit {
var m : HashMap<Any?, Any?>? = HashMap()
m?.put(1, 1)
}
open fun test2() : Unit {
var m : HashMap<*, *>? = HashMap()
var g : G<String?>? = G("")
var g2 : G<String?>? = G<String?>("")
}
}