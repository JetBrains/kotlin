import java.util.BitSet
open class Foo() {
open fun foo(o : BitSet?) : Unit {
var o2 : BitSet? = o
var foo : Int = 0
foo = o2?.size()!!
}
}