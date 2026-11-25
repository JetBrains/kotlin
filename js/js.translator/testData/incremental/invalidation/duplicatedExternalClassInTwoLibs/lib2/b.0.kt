package p

external open class Event

fun bVersion() = 1
fun bUse(e: Event?) = e == null
