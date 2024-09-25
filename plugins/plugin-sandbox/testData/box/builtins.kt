// WITH_STDLIB

fun foo1() {}

fun foo2(): Int = 2

fun foo3(): Short = 3

fun foo4(): Byte = 4

fun foo5(): Long = 5L

fun foo6(): Float = 6.0f

fun foo7(): Double = 7.0

fun foo8(): Char = '8'

fun foo9(): Boolean = true

fun foo10(): String = "10"

fun foo11(): List<String> = listOf("11")

fun foo12(): MutableList<String> = mutableListOf("12")

fun foo13(): Any = 13

fun foo14(): Nothing = TODO()

fun foo15(): () -> Int = { 15 }

fun foo16(): Array<String> = arrayOf("16")

fun foo17(): Number = 17

fun foo18(): AnnotationTarget = AnnotationTarget.CLASS

fun box() = "OK"
