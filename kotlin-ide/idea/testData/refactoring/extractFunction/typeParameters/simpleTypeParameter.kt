// PARAM_TYPES: V
// PARAM_DESCRIPTOR: value-parameter v: V defined in foo
open class Data(val x: Int)

class Pair<A, B>(val a: A, val b: B)

// SIBLING:
fun <V: Data> foo(v: V): Pair<Int, V> {
    return <selection>Pair(v.x + 10, v)</selection>
}