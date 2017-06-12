// EXPECTED_REACHABLE_NODES: 887
package foo


fun box(): String {
    var threwForEmptyList = false

    val arr = ArrayList<Int>()
    try {
        arr.removeAt(2)
    }
    catch(e: IndexOutOfBoundsException) {
        threwForEmptyList = true
    }

    for (i in 0..10) {
        arr.add(i)
    }

    var threwForFilled = false

    try {
        arr.removeAt(20)
    }
    catch(e: IndexOutOfBoundsException) {
        threwForFilled = true
    }

    return if (threwForEmptyList && threwForFilled) "OK" else "fail"
}