package test1

fun param(x: String?): Int = x?.length ?: 0

fun result(): Number = 1

fun defaultValue(): Int = 1

fun trailing(block: () -> Int): Int = block()

fun <T> generic(x: T): T = x

fun <T> genericAddParam(x: T, y: T): T = x
