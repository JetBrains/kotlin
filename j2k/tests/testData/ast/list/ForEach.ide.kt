import java.util.*
public open class ForEach() {
public open fun test() : Unit {
val xs = ArrayList<Any>()
val ys = LinkedList<Any>()
for (x in xs)
{
ys.add(x)
}
for (y in ys)
{
xs.add(y)
}
}
}