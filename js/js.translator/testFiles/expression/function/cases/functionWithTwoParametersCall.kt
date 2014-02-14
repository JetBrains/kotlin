package foo

fun sum(param1: Int, param2: Int): Int {
    return param1 + param2;
}

fun box(): Boolean {
    return (sum(1, 5) == 6)
}