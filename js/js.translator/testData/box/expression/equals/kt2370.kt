// EXPECTED_REACHABLE_NODES: 886
package foo


fun box(): String {
    val data = ArrayList<String>()
    data.add("foo")
    data.add("bar")
    data.add("whatnot")
    val data2 = ArrayList<String>()
    data2.addAll(data)
    return if (data.equals(data2)) "OK" else "fail"
}
