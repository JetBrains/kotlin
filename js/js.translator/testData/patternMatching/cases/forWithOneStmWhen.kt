package foo


fun box(): Boolean {
    var result = false
    for (i in array(1))
        when (i) {
            1 -> result = true
            else -> result = false
        }
    return result;
}

