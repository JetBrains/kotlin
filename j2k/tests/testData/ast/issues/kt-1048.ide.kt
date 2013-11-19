import java.util.HashMap
open class G<T : String>(t : T) {
}
public open class Java() {
open fun test() {
val m = HashMap()
m.put(1, 1)
}
open fun test2() {
val m = HashMap()
val g = G("")
val g2 = G<String>("")
}
}