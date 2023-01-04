inline fun foo(arr: IntArray, transform: (Int) -> Int): Int {
    var sum = 0
    for (x in arr) sum += transform(x)
    return sum
}