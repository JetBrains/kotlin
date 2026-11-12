package test

@JvmInline
value class Id(val value: String)

fun topLevel(id: Id): String = id.value
