// TARGET_BACKEND: JVM_IR

// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*

fun local(): String {
    @Serializable
    class X(val t: String)

    return X.serializer().descriptor.serialName
}

fun localGeneric(): String {
    @Serializable
    class X<T>(val t: T)

    return X.serializer(String.serializer()).descriptor.serialName
}

fun box(): String {
    val l1 = local()
    val lg = localGeneric()
    if (l1 != "local.X") return l1
    if (lg != "localGeneric.X") return lg
    return "OK"
}
