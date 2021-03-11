fun test(list: List<Int>) {
    list.mapNotNull {
        if (it == 0) {
            <selection><caret>return@mapNotNull</selection> null
        }
        it
    }
}