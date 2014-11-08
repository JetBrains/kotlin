package foo

fun box() : String {
    val c = MyCollection1()

    var sum = 0
    for (n in c, i in n) {
        sum = sum + i
    }

    if(sum != 10) return "FAIL: $sum"
    return "OK"
}

class MyCollection1(): Iterable<Int> {
    override fun iterator() = object: Iterator<Int> {
        var k : Int = 3

        override fun next() : Int = k--
        override fun hasNext() = k > 0
    }
}

fun Int.iterator(): Iterator<Int> = object: Iterator<Int> {
    var k : Int = this@iterator

    override fun next() : Int = k--
    override fun hasNext() = k > 0
}