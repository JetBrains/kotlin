// EXPECTED_REACHABLE_NODES: 891
package foo


fun box(): String {
    val oneTwo = Array(2) {
        it + 1
    }
    val a = ArrayList<() -> Int>()
    for (i in oneTwo) {
        for (l in 1..2) {
            val j = l
            a.add({
                      var res = 0
                      for (t in 0..2) {
                          res += i * j
                      }
                      res
                  })
        }
    }
    var sum = 0
    for (f in a) {
        sum += f()
    }

    if (sum != 27) return "fail: $sum"

    return "OK"
}