// TARGET_BACKEND: JVM_IR

// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.modules.*
import java.lang.AssertionError
import java.lang.IllegalArgumentException

interface I

@Serializable
data class Box<T: I>(val boxed: T)

inline fun <reified T: Any> getSer(): KSerializer<T> {
    return serializer<T>()
}

inline fun <reified T: Any> getListSer(): KSerializer<List<T>> {
    return serializer<List<T>>()
}

fun checkBlock(name: String, block:() -> Unit) {
    try {
        block()
    } catch (e: IllegalArgumentException) {
        if (!e.message!!.contains("Star projections in type arguments are not allowed")) throw e
        return
    }
    throw AssertionError("Expected exception to be thrown in block $name")
}

fun box(): String {
    checkBlock("direct") {
        serializer<Box<*>>()
    }
    checkBlock("direct list") {
        serializer<List<Box<*>>>()
    }
    checkBlock("getSer") {
        getSer<Box<*>>()
    }
    checkBlock("getListSer") {
        getListSer<Box<*>>()
    }
    return "OK"
}
