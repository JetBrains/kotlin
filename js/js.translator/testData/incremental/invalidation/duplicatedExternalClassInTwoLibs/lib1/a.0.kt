package p

external open class Event

fun aVersion() = 1
fun aUse(e: Event?) = e == null
