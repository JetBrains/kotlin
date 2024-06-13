// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: ReferenceTypes
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
