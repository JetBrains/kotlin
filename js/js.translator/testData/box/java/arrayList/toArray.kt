// EXPECTED_REACHABLE_NODES: 887
package foo


fun box(): String {
    var i = 0
    val list = ArrayList<Int>()
    while (i++ < 3) {
        list.add(i)
    }

    // test addAt
    list.add(1, 500)

    val array = list.toTypedArray()

    return if (array[0] == 1 && array[1] == 500 && array[2] == 2 && array[3] == 3 && JSON.stringify(list) == "[1,500,2,3]") "OK" else "fail"
}