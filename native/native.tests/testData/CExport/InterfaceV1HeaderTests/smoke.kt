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