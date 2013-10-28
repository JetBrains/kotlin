import java.util.*
public open class ForEach() {
public open fun test() : Unit {
var xs : ArrayList<Any?>? = ArrayList<Any?>()
var ys : MutableList<Any?>? = LinkedList<Any?>()
for (x : Any? in xs!!)
{
ys?.add(x)
}
for (y : Any? in ys!!)
{
xs?.add(y)
}
}
}