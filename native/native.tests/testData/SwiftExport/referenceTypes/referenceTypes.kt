// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: ReferenceTypes(deps)
// FILE: Bar.kt
class Bar(var foo: Foo) {
    fun getAndSetFoo(newFoo: Foo): Foo {
        val oldFoo = foo
        foo = newFoo
        return oldFoo
    }
}
// FILE: Baz.kt
object Baz {
    var x = 0
    var foo = Foo(0)

    fun getAndSetX(newX: Int): Int {
        val oldX = x
        x = newX
        return oldX
    }

    fun getAndSetFoo(newFoo: Foo): Foo {
        val oldFoo = foo
        foo = newFoo
        return oldFoo
    }
}

fun getBaz() = Baz

// FILE: Foo.kt
class Foo(var x: Int) {
    constructor(f: Float) : this(f.toInt())

    fun getAndSetX(newX: Int): Int {
        val oldX = x
        x = newX
        return oldX
    }
}

fun getX(foo: Foo) = foo.x

fun makeFoo(x: Int) = Foo(x)

fun idFoo(foo: Foo) = foo

var globalFoo = Foo(42)

val readGlobalFoo
    get() = globalFoo

fun getGlobalFoo() = globalFoo

typealias FooAsTypealias = Foo

// FILE: Permanent.kt
import kotlin.native.internal.isPermanent

object Permanent

fun getPermanentId(permanent: Permanent) = permanent.hashCode()

fun idPermanent(permanent: Permanent) = permanent

fun getGlobalPermanent(): Permanent {
    check(Permanent.isPermanent())
    return Permanent
}

// FILE: Any.kt
annotation class Class() // intentially unsupported

val instance = Class()

object Object {
    val instance: Any
        get() = Object

    fun isInstance(obj: Any) = obj == Object
}

class SomeFoo(var storage: Any)
class SomeBar
class SomeBaz

fun isMainObject(obj: Any): Boolean = obj == instance

val mainObject: Any get() = instance

fun isMainPermanentObject(obj: Any): Boolean = obj == Object

fun getMainPermanentObject(): Any = Object

// FILE: KotlinAnyMethodsx.kt
class HashableObject(val value: Int) {
    override fun hashCode(): Int = value
    override fun equals(other: Any?): Boolean = (other as? HashableObject)?.value == value || other as? Int? == value
    override fun toString(): String = "$value"
}

fun getHashableObject(value: Int): Any = HashableObject(value)
fun getHash(obj: Any): Int = obj.hashCode()
fun isEqual(lhs: Any, rhs: Any): Boolean = lhs == rhs

// FILE: dependency_usage.kt
import dependency.*

val deps_instance: Any = DepsFoo()

fun isDepsObject(obj: Any): Boolean = obj is DepsFoo
fun isSavedDepsObject(obj: Any): Boolean = obj == deps_instance

// MODULE: second_main(deps)
// FILE: second_main.kt

import dependency.*

val deps_instance_2: Any = DepsFoo()

fun isDepsObject_2(obj: Any): Boolean = obj is DepsFoo
fun isSavedDepsObject_2(obj: Any): Boolean = obj == deps_instance_2

// MODULE: deps
// FILE: deps_file.kt
package dependency

class DepsFoo
