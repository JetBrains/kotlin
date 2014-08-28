package foo

fun box(): Boolean {


    (when (1) {
        3 -> {
            3
        }
        1 -> {
            throw Exception();
        }
        else -> {
            return false
        }
    })

    return false
}