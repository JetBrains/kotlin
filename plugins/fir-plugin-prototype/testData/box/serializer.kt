// FIR_DISABLE_LAZY_RESOLVE_CHECKS
import org.jetbrains.kotlin.fir.plugin.CoreSerializer
import org.jetbrains.kotlin.fir.plugin.MySerializable

@CoreSerializer
object FirstSerializer

@CoreSerializer
object SecondSerializer

@MySerializable
class A

@MySerializable
class B

@MySerializable
class C

@MySerializable
class D

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
