// LANGUAGE: +FullValueClasses
// SNIPPET

@JvmInline
value class Meters(val value: Int)

value class Point(val x: Int, val y: Int)

// SNIPPET

val res = "${Meters(1).value}_${Point(2, 3).y}"

// EXPECTED: res == "1_3"
