// WITH_STDLIB

@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class Id(val value: String)

@JvmExposeBoxed
fun topLevel(id: Id): String = id.value

class Host @JvmExposeBoxed constructor(val id: Id) {
    @JvmExposeBoxed
    fun member(other: Id): Id = other

    @get:JvmExposeBoxed
    @set:JvmExposeBoxed
    var accessors: Id = Id("")
}
