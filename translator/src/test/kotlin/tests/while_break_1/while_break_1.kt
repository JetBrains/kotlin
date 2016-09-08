fun while_break_1(): Int {
    var i = 1
    while (true) {
        i = i + 1
        break
        i = -100
    }
    return i
}