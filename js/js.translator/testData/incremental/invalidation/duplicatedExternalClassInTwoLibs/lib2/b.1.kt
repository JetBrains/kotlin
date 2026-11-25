package p

external open class Event

fun bVersion() = 2
fun bUse(e: Event?) = e == null
