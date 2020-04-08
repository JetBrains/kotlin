fun test(list: List<Int>) {
    list.mapNotNull {
        if (it == 0) {
            <caret>return@mapNotNull null
        }
        it
    }
}