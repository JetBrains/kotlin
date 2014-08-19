package foo


fun box(): Boolean {

    return (when(1) {
        2 -> 3
        1 -> 1
        else -> 5
    } == 1)

}