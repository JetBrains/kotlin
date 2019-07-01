fun bar(map: HashMap<String, Int>, list1: List<Int>, list2: List<String>) {
    for (entry: MutableMap.MutableEntry<String, Int> in map.entries) {
        val value: Int = entry.value
        if (entry.key == null) {
            println(value + 1)
        }
    }

    for (i: Int in list1) {
        i + 1
    }

    for (i: String in list2) {
        i == null
    }
}