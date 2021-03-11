fun foo() {
    for (i in 1..10) {
        for (j: Int in collection.filter(predicate)) {
            for ((x, y) in entries) {
                <caret>
            }
        }
    }
}