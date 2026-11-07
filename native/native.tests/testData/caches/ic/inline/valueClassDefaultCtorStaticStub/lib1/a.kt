package test

internal inline fun g(): Any = probe()

private inline fun probe(): Any = V()

private value class V(val s: String = "five")
