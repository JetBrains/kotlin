run {
    val i = 0
    while (i < 0) {
        run {
            val i = 1
            i++
        }
        j++
        i++
    }
}