// EXPECTED_REACHABLE_NODES: 1214
// KT-2468 ArrayList<String> is List<String> or HashSet<String> is Set<String> fails in generated JS code
package foo

class A

fun checkAbstractList(obj: Any) {
    assertTrue(obj is AbstractMutableList<*>, "checkAbstractList: is AbstractMutableList")
    assertTrue(obj is MutableList<*>, "checkAbstractList: is MutableList")
    assertTrue(obj is List<*>, "checkAbstractList: is List")
    assertTrue(obj is AbstractMutableCollection<*>, "checkAbstractList: is AbstractMutableCollection")
    assertTrue(obj is MutableCollection<*>, "checkAbstractList: is MutableCollection")
    assertTrue(obj is Collection<*>, "checkAbstractList: is Collection")
    assertTrue(obj is MutableIterable<*>, "checkAbstractList: is MutableIterable")
    assertTrue(obj is Iterable<*>, "checkAbstractList: is Iterable")
    assertTrue((obj as Iterable<*>).iterator() is Iterator, "checkAbstractList: iterator() is Iterator")
}

fun checkArrayList(obj: Any) {
    assertTrue(obj is ArrayList<*>, "checkArrayList: is ArrayList")
    assertTrue((obj as Iterable<*>).iterator() is MutableIterator, "checkAbstractList: iterator() is MutableIterator")
    checkAbstractList(obj)
}

fun checkHashSet(obj: Any) {
    assertTrue(obj is HashSet<*>, "checkHashSet: is HashSet")
    assertTrue(obj is AbstractMutableSet<*>, "checkHashSet: is AbstractMutableSet")
    assertTrue(obj is AbstractMutableCollection<*>, "checkHashSet: is AbstractMutableCollection")
    assertTrue(obj is MutableCollection<*>, "checkHashSet: is MutableCollection")
    assertTrue(obj is Collection<*>, "checkHashSet: is Collection")
    assertTrue(obj is MutableIterable<*>, "checkHashSet: is MutableIterable")
    assertTrue(obj is Iterable<*>, "checkHashSet: is Iterable")
    assertTrue(obj is MutableSet<*>, "checkHashSet: is MutableSet")
    assertTrue(obj is Set<*>, "checkHashSet: is Set")
    assertTrue((obj as Set<*>).iterator() is Iterator, "checkHashSet: iterator() is Iterator")
    assertTrue((obj as Set<*>).iterator() is MutableIterator, "checkHashSet: iterator() is MutableIterator")
}

fun checkLinkedHashSet(obj: Any) {
    assertTrue(obj is LinkedHashSet<*>, "checkLinkedHashSet: is LinkedHashSet")
    checkHashSet(obj)
}

fun checkHashMap(obj: Any) {
    assertTrue(obj is HashMap<*, *>, "checkHashMap: is HashMap")
    assertTrue(obj is MutableMap<*, *>, "checkHashMap: is MutableMap")
    assertTrue(obj is Map<*, *>, "checkHashMap: is Map")
    assertTrue((obj as Map<*, *>).values is Collection, "checkHashMap: values is Collection")
    assertTrue((obj as Map<*, *>).keys is Set, "checkHashMap: keys is Set")
}

fun checkLinkedHashMap(obj: Any) {
    assertTrue(obj is LinkedHashMap<*, *>, "checkLinkedHashMap: is LinkedHashMap")
    checkHashMap(obj)
}

fun box(): String {

    checkArrayList(ArrayList<Int>())
    checkArrayList(arrayListOf(1, 2, 3))

    checkArrayList(ArrayList<String>())
    checkArrayList(arrayListOf("first", "second"))

    checkArrayList(ArrayList<A>())
    checkArrayList(arrayListOf(A()))

    checkHashSet(HashSet<Int>())
    checkHashSet(hashSetOf(1))
    checkLinkedHashSet(LinkedHashSet<Int>(0))

    checkHashSet(HashSet<Double>())
    checkHashSet(hashSetOf(1.0))
    checkLinkedHashSet(LinkedHashSet<Double>(0))

    checkHashSet(HashSet<String>())
    checkHashSet(hashSetOf("test"))
    checkLinkedHashSet(LinkedHashSet<String>(0))

    checkHashSet(HashSet<A>())
    checkHashSet(hashSetOf(A()))
    checkLinkedHashSet(LinkedHashSet<A>(0))

    checkHashMap(HashMap<Int, String>())
    checkHashMap(hashMapOf(1 to "test"))
    checkLinkedHashMap(LinkedHashMap<Int, String>())

    checkHashMap(HashMap<Double, String>())
    checkHashMap(hashMapOf(1.0 to "test"))
    checkLinkedHashMap(LinkedHashMap<Double, String>())

    checkHashMap(HashMap<String, String>())
    checkHashMap(HashMap<A, String>())
    checkLinkedHashMap(LinkedHashMap<A, String>())

    return "OK"
}