run {
    var i = 0
    while (i < 0) {
        run {
            var i = 1
            i++
        }
        j++
        i++
    }
}