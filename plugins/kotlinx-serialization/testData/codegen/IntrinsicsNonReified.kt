// CURIOUS_ABOUT: inner, outer
// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.reflect.typeOf

@Serializable
class Box<T>(val t: T)

inline fun <reified T> inner() = serializer<T>()

fun <T> outer() = inner<Box<T>>()
