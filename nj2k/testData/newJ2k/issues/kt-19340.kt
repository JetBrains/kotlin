package test.nullabilityOnClassBoundaries

class Item {
    private var s1: String? = null
    private var s2: String? = null
    operator fun set(s1: String?, s2: String?) {
        this.s1 = s1
        this.s2 = s2
    }
}

class Reader {
    fun readItem(n: Int): Item {
        val item = Item()
        item[readString(n)] = null
        return item
    }

    fun readString(n: Int): String? {
        return if (n <= 0) null else Integer.toString(n)
    }
}
