// TARGET_BACKEND: JVM_IR

// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.modules.*
import java.lang.AssertionError
import java.lang.IllegalArgumentException

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.reflect.typeOf

@Serializable
class Box<T>(val t: T)

inline fun <reified T> inner() = serializer<T>()

fun <T> outer() = inner<Box<T>>()

fun checkBlock(name: String, block:() -> Unit) {
    try {
        block()
    } catch (e: IllegalArgumentException) {
        if (!e.message!!.contains("Captured type parameter T of <root>.IntrinsicsNonReifiedKt.outer from generic non-reified function.")) throw e
        return
    }
    throw AssertionError("Expected exception to be thrown in block $name")
}

fun box(): String {
    checkBlock("string") {
        outer<String>()
    }
    checkBlock("list string") {
        outer<List<String>>()
    }
    return "OK"
}
