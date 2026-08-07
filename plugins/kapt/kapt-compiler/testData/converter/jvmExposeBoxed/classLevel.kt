// WITH_STDLIB

@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class Id(val value: String)

// A class-level annotation reaches every member that can be exposed, but stops at the class boundary: the
// nested class, the inner class and the companion object are not covered by it.
@JvmExposeBoxed
class ClassLevel(val id: Id) {
    fun member(other: Id): Id = other

    class Nested {
        fun member(id: Id): Id = id
    }

    inner class Inner {
        fun member(id: Id): Id = id
    }

    companion object {
        fun companionMember(id: Id): Id = id
    }
}
