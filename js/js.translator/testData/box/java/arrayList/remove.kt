// EXPECTED_REACHABLE_NODES: 886
package foo


fun box(): String {
    val arr = ArrayList<Int>()
    for (i in 0..5) {
        arr.add(i)
    }

    val removedElement = arr.removeAt(2)
    val removed = arr.remove(4)
    if (arr.size != 4) return "fail1: ${arr.size}"
    if (removedElement != 2) return "fail2: ${removedElement}"
    if (!removed) return "fail3"
    if (arr[0] != 0) return "fail4: ${arr[0]}"
    if (arr[1] != 1) return "fail5: ${arr[1]}"
    if (arr[2] != 3) return "fail6: ${arr[2]}"
    if (arr[3] != 5) return "fail7: ${arr[3]}"

    return "OK"
}