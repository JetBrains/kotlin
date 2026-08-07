// WITH_STDLIB
// JVM_EXPOSE_BOXED

// Whole-module exposure has to reach stub generation as well: every boxed variant below is produced by
// '-Xjvm-expose-boxed' alone, with no annotation written anywhere. Unlike a class-level annotation, the flag
// also covers nested classes, inner classes and companion objects.

@JvmInline
value class Id(val value: String)

fun topLevel(id: Id): String = id.value

class Host(val id: Id) {
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

// Declarations the flag has to skip without reporting anything.
suspend fun suspendMember(id: Id): Id = id

private fun privateMember(id: Id): Id = id
