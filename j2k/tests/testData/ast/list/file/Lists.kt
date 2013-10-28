import java.util.*
public open class Lists() {
public open fun test() : Unit {
var xs : MutableList<Any?>? = ArrayList<Any?>()
var ys : MutableList<Any?>? = LinkedList<Any?>()
var zs : ArrayList<Any?>? = ArrayList<Any?>()
xs?.add(null)
ys?.add(null)
xs?.clear()
ys?.clear()
zs?.add(null)
}
}