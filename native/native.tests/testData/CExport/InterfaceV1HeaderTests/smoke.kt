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
