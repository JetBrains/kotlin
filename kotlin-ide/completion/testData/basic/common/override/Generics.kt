interface I<T> {
    fun foo(t: T): T
}

class A<T> : List<String>, I<T> {
    o<caret>
}

// EXIST: { lookupString: "override", itemText: "override" }
// EXIST: { itemText: "override fun hashCode(): Int {...}", tailText: null, typeText: "Any", attributes: "" }
// EXIST: { itemText: "override fun foo(t: T): T {...}", tailText: null, typeText: "I", attributes: "bold" }
// EXIST: { itemText: "override fun get(index: Int): String {...}", tailText: null, typeText: "List", attributes: "bold" }
