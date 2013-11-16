import java.util.BitSet
open class Foo() {
open fun foo(o : BitSet) : Unit {
val o2 = o
val foo = 0
foo = o2.size()
}
}