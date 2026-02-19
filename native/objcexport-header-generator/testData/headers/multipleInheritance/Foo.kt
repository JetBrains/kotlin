/**
 * inheritance chain: A > B > C
 */
abstract class A<T> {
    abstract fun foo(): T
}

open class B<T> : A<Int>() {
    override fun foo(): Int = 42
}

open class C : B<Int>() {
    override fun foo(): Int = 42
}

/**
 * inheritance tree:
 * RootA  RootB
 *     \ /
 *    Tree
 */
interface RootA<T> {
    fun fooA(): T
}

interface RootB<T> {
    fun fooB(): T
}

class Tree : RootA<Int>, RootB<String> {
    override fun fooA(): Int = 42
    override fun fooB(): String = "42"
}