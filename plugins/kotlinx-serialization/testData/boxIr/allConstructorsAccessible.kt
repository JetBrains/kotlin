// TARGET_BACKEND: JVM_IR
// WITH_REFLECT

import kotlinx.serialization.Serializable
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.kotlinFunction

@Serializable
data class Tuple2<out A1, out A2>(
    val _1: A1,
    val _2: A2,
)

fun box(): String {
    val cls = Tuple2::class
    val ctor = cls.constructors.single { it.parameters.size == 4 }
    val kf = ctor.javaConstructor?.kotlinFunction
    return if (kf.toString() == "fun `<init>`(kotlin.Int, A1?, A2?, kotlinx.serialization.internal.SerializationConstructorMarker?): Tuple2<A1, A2>") "OK" else "Fail: $kf"
}
