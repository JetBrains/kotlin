// EXPECTED_REACHABLE_NODES: 890

class World() {
    public val items: ArrayList<Item> = ArrayList<Item>()

    inner class Item() {
        init {
            items.add(this)
        }
    }

    val foo = Item()
}

fun box() : String {
    val w = World()
    if (w.items.size != 1) return "fail"
    return "OK"
}
