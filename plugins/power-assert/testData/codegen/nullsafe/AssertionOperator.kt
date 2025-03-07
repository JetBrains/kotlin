fun box(): String {
    return test1(false) +
            test2("test") +
            test3(Holder("Test")) +
            test4(mapOf("a" to 42)) +
            test5(Node(Node(null)))
}

fun test1(a: Boolean?) = expectThrowableMessage {
    assert(a!!)
}

fun test2(a: String?) = expectThrowableMessage {
    assert(a!!.length == 5)
}

class Holder(val value: String?) {
    override fun toString(): String = "Holder"
}

fun test3(a: Holder) = expectThrowableMessage {
    assert(a.value!!.length == 5)
}

fun test4(a: Map<String, Int?>) = expectThrowableMessage {
    assert(a["a"]!! == 43)
}

class Node(val next: Node?) {
    override fun toString(): String = "Node"
}

fun test5(a: Node?) = expectThrowableMessage {
    assert(a!!.next!!.next != null)
}