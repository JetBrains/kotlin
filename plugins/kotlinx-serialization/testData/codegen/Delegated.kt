// CURIOUS_ABOUT: <init>, <clinit>
// WITH_STDLIB

import kotlinx.serialization.*

fun interface A {
    fun getText(): String
}

fun generateImpl() = A { "Hello, world!" }

@Serializable
class Test : A by generateImpl()
