package foo

fun box(): Boolean {
    var i = 0
    while ( i < 100) {
        if (i == 3) {
            break;
        }
        ++i;
    }

    return i == 3
}