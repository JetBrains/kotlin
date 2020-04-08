fun test() {
    val items = sequenceOf<Any>()
    items.forEach {
        ForEachable().forEach {}
    }
}

class ForEachable {
    fun forEach(action: (ForEachable) -> Unit) {}
}