// DUMP_IR

import org.jetbrains.kotlin.fir.plugin.CoreSerializer
import org.jetbrains.kotlin.fir.plugin.MySerializable

@CoreSerializer
object FirstSerializer {
    fun bFunction() {}
    fun aFunction() {}

    val bProp = 1
    val aProp = 2
}

@CoreSerializer
object SecondSerializer

@MySerializable
class D

@MySerializable
class C

@MySerializable
class B

@MySerializable
class A

fun testFirstSerializer() {
    FirstSerializer.serializeA(A())
    FirstSerializer.serializeB(B())
    FirstSerializer.serializeC(C())
    FirstSerializer.serializeD(D())
}

fun testSecondSerializer() {
    SecondSerializer.serializeA(A())
    SecondSerializer.serializeB(B())
    SecondSerializer.serializeC(C())
    SecondSerializer.serializeD(D())
}

fun box(): String {
    testFirstSerializer()
    testSecondSerializer()
    return "OK"
}
