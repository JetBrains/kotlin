// WITH_RUNTIME
// IS_APPLICABLE: false
// IS_APPLICABLE_2: false
fun foo(list: List<MutableCollection<Int>>) {
    <caret>for (collection in list) {
        collection.add(collection.size)
    }
}