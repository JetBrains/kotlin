// WITH_STDLIB
// IGNORE_BACKEND_K1: JVM_IR
// K1 did not allow star projections, any compilable code with Sealed<*> uses K2.

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.descriptors.*

@Serializable
sealed class Param<T> {
    abstract val value: T
}

@Serializable
class S(override val value: String): Param<String>()

@Serializable
class I(override val value: Int): Param<Int>()

@Serializable
class Wrapper(val param: Param<*>)

@Serializable
abstract class Poly<T> {
    abstract val value: T
}

@Serializable
class Wrapper2(val p: Poly<*>)

fun box(): String {
    val s1 = Json.encodeToString(Wrapper(S("str")))
    val s2 = Json.encodeToString(Wrapper(I(11)))
    val s3 = Wrapper2.serializer().descriptor.getElementDescriptor(0).serialName
    if (s1 != "{\"param\":{\"type\":\"S\",\"value\":\"str\"}}") return s1
    if (s2 != "{\"param\":{\"type\":\"I\",\"value\":11}}") return s2
    if (s3 != "kotlinx.serialization.Polymorphic<Poly>") return s3
    return "OK"
}
