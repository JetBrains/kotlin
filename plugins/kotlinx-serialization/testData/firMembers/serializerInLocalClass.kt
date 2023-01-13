// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

import kotlinx.serialization.*

fun local(): String {
    @Serializable
    data class Carrier(val i: Int)

    return Carrier.serializer().descriptor.toString()
}

fun box(): String {
    val expected = "local.Carrier(i: kotlin.Int)"
    val actual = local()
    if (expected != actual) {
        return "Fail: $actual"
    }
    return "OK"
}
