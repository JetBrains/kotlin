package foo

fun box(): Boolean {
    var i = 0
    var b = true
    do {
        ++i;
        if (i >= 1) {
            continue;
        }
        b = false;
    }
    while (i < 100)

    return b
}