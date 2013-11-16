package demo
open class Collection<E>(e : E) {
{
System.out.println(e)
}
}
open class Test() {
open fun main() : Unit {
val raw1 = Collection(1)
val raw2 = Collection<Int>(1)
val raw3 = Collection<String>("1")
}
}