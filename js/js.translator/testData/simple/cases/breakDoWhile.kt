package foo

fun box(): Boolean {
    var i = 0
    do {
        if (i == 3) {
            break;
        }
        ++i;
    }
    while ( i < 100)

    return i == 3
}