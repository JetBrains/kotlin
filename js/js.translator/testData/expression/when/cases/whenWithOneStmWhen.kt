package foo


fun box(): Boolean {
    var result = false
    val i = 1
    when (i) {
        1 ->
            when (i) {
                1 ->    result = true
                else -> result = false
            }

        else ->
            when (i) {
                1 ->    result = true
                else -> result = false
            }
    }

    return result;
}
