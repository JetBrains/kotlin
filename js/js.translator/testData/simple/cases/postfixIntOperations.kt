package foo

fun box(): Boolean {
    var a = 3;
    val b = a++;
    a--;
    a--;
    return (a++ == 2) && (b == 3)
}

