import java.util.*
import java.io.*

class World() {
    public val items: ArrayList<Item> = ArrayList<Item>

    class Item() {
        {
            items.add(this)
        }
    }

    val foo = Item()
}

fun box(): String {
    val w = World()
    if (w.items.size() != 1) return "fail"
    return "OK"
}
