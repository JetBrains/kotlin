// !IGNORE_FIR

val a = readLine()

val b = when(a) {
    "abc" -> 1
    "def", "ghi" -> 2
    else -> 3
}

when(a) {
    "abc1" -> println(1)
    "def1", "ghi1" -> println(2)
}
