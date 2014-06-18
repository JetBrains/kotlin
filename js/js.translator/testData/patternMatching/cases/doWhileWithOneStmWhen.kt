package foo


fun box(): Boolean {
    var result = false
    var i = 1
    do
        when (i) {
            1 ->    result = true
            else -> result = false
        }
    while (i==0)
    return result;
}

