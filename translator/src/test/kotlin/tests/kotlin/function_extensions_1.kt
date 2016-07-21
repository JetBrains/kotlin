fun Int.compareTo(x: Int) : Int{
    return this + x + 5
}

fun function_extensions_1(x : Int): Int {
    return x.compareTo(1) + 39.compareTo(123)
}
