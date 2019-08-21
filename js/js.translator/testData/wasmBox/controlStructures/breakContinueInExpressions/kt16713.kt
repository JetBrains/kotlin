fun poll(): String? =  null

fun next() {
    while (true) {
        poll() ?: break
    }

    while (true) {
        unblock(poll() ?: break)
    }

    while (true) {
        unblock(poll() ?: break)
    }
}

fun unblock(p: String) {

}

fun box() : String {
    next()
    return "OK"
}