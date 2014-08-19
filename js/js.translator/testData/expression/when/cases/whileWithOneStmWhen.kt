package foo


fun box(): Boolean {
    var result = false
    var i = 1
    while(i==1)
        when (i) {
            1 -> { result = true; break }
            else -> result = false
        }
    return result;
}

