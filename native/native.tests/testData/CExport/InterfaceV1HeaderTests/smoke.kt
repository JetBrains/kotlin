package tests.native

const val constDouble: Double = 3.14
const val constFloat: Float = 2.73F
const val constInt: Int = 42
const val constLong: Long = 1984

var variableString: String = "hello"
var variableAnyNullable: Any? = Any()

fun functionWithParams(a: String, b: String): String = "$a $b"

private fun functionPrivate() {}

internal fun functionInternal() {}

suspend fun suspendFun() = 42

suspend fun unitSuspendFun() = Unit

class SimpleClass() {}

data class DataClass(val a: SimpleClass, var b: Int) {}

interface MarkerInterface {}

interface CatInterface {
    fun meow(): String
}

class Tom : CatInterface {
    override fun meow(): String = "sudo rm -rf /"
}

fun CatInterface.interfaceExtension() {}

fun Tom.interfaceExtension() {}

enum class MyEnum {
    A, B, C
}

sealed class SealedClass {
    class A : SealedClass()

    object B : SealedClass()

    open class C : SealedClass() {
        class D : C()
    }
}

value class ValueClass(val content: Int)

fun produceValueClass() = ValueClass(5)

fun consumeValueClass(param: ValueClass): Int = param.content

context(c: Boolean)
fun ctxFoo() {}

context(c: Boolean)
val ctxVal: Int get() = 0

context(c: Boolean)
var ctxVar: Int
    get() = if (c) 1 else 0
    set(v) {}

class CtxClass {
    context(c: Boolean)
    fun ctxFoo() {}

    context(c: Boolean)
    val ctxVal: Int get() = 0

    context(c: Boolean)
    var ctxVar: Int
        get() = if (c) 1 else 0
        set(v) {}
}

class WithCompanionObject {
    companion object {
        val xVal = 0
        var xVar = 0
        val yVal
            get() = 0
        var yVar
            get() = 0
            set(value) {}
        fun f() {}
    }
}

class WithCompanionBlock {
    companion {
        val xVal = 0
        var xVar = 0
        val yVal
            get() = 0
        var yVar
            get() = 0
            set(value) {}
        fun f() {}
    }
}

companion val SimpleClass.xVal = 0
companion var SimpleClass.xVar = 0
companion val SimpleClass.yVal
    get() = 0
companion var SimpleClass.yVar
    get() = 0
    set(value) {}
companion fun SimpleClass.f() {}

// Constructor ordering: K1 reads constructors as `secondary constructors (declaration order) + primary`, while
// the IR lists the primary first, so the IR mode must reorder. Exported order must be (String), (Boolean), (Int).
class MultipleConstructors(val value: Int) {
    constructor(text: String) : this(text.length)
    constructor(flag: Boolean) : this(if (flag) 1 else 0)
}

// Generic extension property: its getter carries the type parameter `T` in IR but the K1 accessor descriptor
// reports none, so the type-parameter exclusion must not drop the accessor.
val <T : Any> List<T>.sizeDoubled: Int
    get() = size * 2

// Overloaded top-level functions declared in non-alphabetical order: both K2 metadata and IR keep declaration
// order, so the exported order must be (String) then (Int), not re-sorted by parameter type.
fun overloaded(text: String): Int = text.length
fun overloaded(number: Int): Int = number
