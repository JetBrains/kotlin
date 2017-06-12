// EXPECTED_REACHABLE_NODES: 886
package foo


fun box(): String {
    var elems = ArrayList<Int>()
    for (i in 0.rangeTo(5)) {
        elems.add(i)
    }
    if (elems[0] != 0) return "fail1: ${elems[0]}"
    if (elems[1] != 1) return "fail2: ${elems[1]}"
    if (elems[2] != 2) return "fail3: ${elems[2]}"
    if (elems[3] != 3) return "fail4: ${elems[3]}"
    if (elems[4] != 4) return "fail5: ${elems[4]}"
    if (elems[5] != 5) return "fail6: ${elems[5]}"

    return "OK"
}