var field: Int = 0

fun next(): Int {
    return ++field
}


fun box(): String {
    val task: String

    do {
        if (next() % 2 == 0) {
            task = "OK"
            break
        }
    }
    while (!false)

    return task
}
