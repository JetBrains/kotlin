package demo
open class Collection<E>(e : E?) {
{
System.out?.println(e)
}
}
open class Test() {
open fun main() : Unit {
var raw1 : Collection<Any?>? = Collection(1)
var raw2 : Collection<Any?>? = Collection<Int?>(1)
var raw3 : Collection<Any?>? = Collection<String?>("1")
}
}