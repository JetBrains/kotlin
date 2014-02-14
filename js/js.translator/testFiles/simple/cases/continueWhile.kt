package foo

fun box(): Boolean {
    var i = 0
    var b = true
    while (i < 100) {
        ++i;
        if (i >= 1) {
            continue;
        }
        b = false;
    }

    return b
}