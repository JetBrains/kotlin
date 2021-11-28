package test

private val x = 1

// We don't yet strip out empty top-level classes from the kotlin_module files!
val y = 2

object A { fun f() = x }
