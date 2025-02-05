// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: ReferenceTypes(deps)
// FILE: Enum.kt

enum class Enum(var i: Int, internal val s: String) {
    a(1, "str") {
        override fun print(): String = "$i - $s"
    },
    b(5, "rts") {
        override fun print(): String = "$s - $i"
    };

    abstract fun print(): String
}

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

// FILE: DataClass.kt
data class DataClass(val i: Int, val s: String)

// FILE: Foo.kt
class Foo(var x: Int) {
    constructor(f: Float) : this(f.toInt())

    fun getAndSetX(newX: Int): Int {
        val oldX = x
        x = newX
        return oldX
    }

    fun Int.memberExt(): Int = x + this

    var Int.memberExtProp: Int
        get() = x + this
        set(v) {
            x = v - this
        }
}

fun getX(foo: Foo) = foo.x

fun Foo.extGetX() = x

var Foo.extX: Int
    get() = x
    set(v) {
        x = v
    }

fun makeFoo(x: Int) = Foo(x)

fun idFoo(foo: Foo) = foo

fun Foo.extId() = this

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

// FILE: OpenClasses.kt

open class Base {
    fun test(): Int = 42
}

open class Derived : Base()

var polymorphicObject: Base = Derived()

fun identity(obj: Base): Base = obj

fun getDerived(): Derived = Derived()
fun getBase(): Base = Base()

abstract class Abstract
class Impl : Abstract()
private class PrivateImpl : Abstract()

var abstractPolymorphicObject: Abstract = Impl()

fun getImpl(): Abstract = Impl()
fun getPrivateImpl(): Abstract = PrivateImpl()

// FILE: dependency_usage.kt
import dependency.*

val deps_instance: Any = DepsFoo()

fun isDepsObject(obj: Any): Boolean = obj is DepsFoo
fun isSavedDepsObject(obj: Any): Boolean = obj == deps_instance

// FILE: factory.kt
class ClassWithFactory(val value: Int)
fun ClassWithFactory(longValue: Long) = ClassWithFactory(longValue.toInt())

// FILE: companion.kt
open class HostBase {
    companion object {
        val hostDepth = 0
    }
}

class HostDerived : HostBase() {
    companion object {
        val hostDepth = 1
    }
}

// MODULE: overrides
// EXPORT_TO_SWIFT
// FILE: overrides.kt

open class Parent(val value: String) {
    open fun foo(): String = "Parent"
    open var bar: Int = 10

    open fun hop(): String = "Parent"
    open fun chain(): String = "Parent"

    open fun poly(): Parent = this
    open fun nullable(): Parent? = this
}

open class Child(value: Int) : Parent("$value") {
    override fun foo(): String = "Child"
    override var bar: Int = 20

    /* override fun hop(): String = "Skipped" */
    override fun chain(): String = "Child"

    override fun poly(): Parent = this
    override fun nullable(): Parent? = this
}

class GrandChild(value: Short) : Child(value.toInt()) {
    final override fun foo(): String = "GrandChild"
    final override var bar: Int
        get() = 42
        set(_) = Unit

    override fun hop(): String = "GrandChild"
    final override fun chain(): String = "GrandChild"

    override fun poly(): Parent = this
    override fun nullable(): Parent = this
}

// MODULE: overrides_across_modules(overrides)
// EXPORT_TO_SWIFT
// FILE: overrides_across_modules.kt

open class Cousin(value: String) : Parent(value) {
    override fun foo(): String = "Cousin"
    override var bar: Int = 21

    /* override fun hop(): String = "Skipped" */
    final override fun chain(): String = "Cousin"

    override fun poly(): Parent = this
    override fun nullable(): Parent? = this
}

abstract class AbstractParent {
    abstract fun foo(): String
}

class AbstractParentImpl : AbstractParent() {
    override fun foo() = "AbstractParentImpl"
}

fun getAbstractParentImpl(): AbstractParent = AbstractParentImpl()

private class AbstractParentPrivateImpl : AbstractParent() {
    override fun foo() = "AbstractParentPrivateImpl"
}

fun getAbstractParentPrivateImpl(): AbstractParent = AbstractParentPrivateImpl()

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
