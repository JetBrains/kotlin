fun box(): String = runAll(
    "test1" to { test1(false) },
    "test2" to { test2("test") },
    "test3" to { test3(Holder("Test")) },
    "test4" to { test4(mapOf("a" to 42)) },
    "test5" to { test5(Node(Node(null))) },
)

fun test1(a: Boolean?) {
    assert(a!!)
}

fun test2(a: String?) {
    assert(a!!.length == 5)
}

class Holder(val value: String?) {
    override fun toString(): String = "Holder"
}

fun test3(a: Holder) {
    assert(a.value!!.length == 5)
}

fun test4(a: Map<String, Int?>) {
    assert(a["a"]!! == 43)
}

class Node(val next: Node?) {
    override fun toString(): String = "Node"
}

fun test5(a: Node?) {
    assert(a!!.next!!.next != null)
}