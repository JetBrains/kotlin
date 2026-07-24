package test1

fun param(x: String): Int = x.length + 1

fun result(): Int = 2

fun defaultValue(x: Int = 2): Int = x

fun trailing(x: Int = 2, block: () -> Int): Int = block() + x

fun <T> generic(x: T): List<T> = listOf(x)

fun <T, R> genericAddParam(x: T, y: R): T = x
