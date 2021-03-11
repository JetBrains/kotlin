fun test() {
    for (i in -2..2) {
        <caret>if (i > 0) i.hashCode()
    }
}