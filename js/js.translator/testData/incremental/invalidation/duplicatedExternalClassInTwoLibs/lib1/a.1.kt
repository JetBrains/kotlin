package p

external open class Event

fun aVersion() = 2
fun aUse(e: Event?) = e == null
