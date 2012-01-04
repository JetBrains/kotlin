package foo


fun box() : Boolean {

    return (when(1) {
        is 2 => 3
        is 1 => 1
        else => 5
    } == 1)

}