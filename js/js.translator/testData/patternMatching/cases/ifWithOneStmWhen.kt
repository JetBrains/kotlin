package foo


fun box(): Boolean {
    var result = false
    var i = 1
    if (i==1)
        when (i) {
            1 ->    result = true
            else -> result = false
        }
    return result;
}