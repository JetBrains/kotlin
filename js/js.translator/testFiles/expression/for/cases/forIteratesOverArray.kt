package foo

val a1 = arrayOfNulls<Int>(10)

fun box() : Boolean {
    var c = 0
    var d = 0
    a1[3] = 3
    a1[5] = 5

    for (a : Int? in a1) {
        if (a != null) {
            c += 1;
        } else {
            d += 1
        }
    }
    return (c == 2) && (d == 8)
}