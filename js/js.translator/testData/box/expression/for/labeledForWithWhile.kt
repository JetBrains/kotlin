// EXPECTED_REACHABLE_NODES: 491
package foo

fun box(): String {
    var n = 0
    forLabel0@ for(i in 0..10) {
        var j = 0
        whileLabel0@ while(j++<i) {
            n++;
            if (j==2)
                continue@forLabel0
        }
    }
    assertEquals(19, n)

    n = 0
    forLabel1@ for(i in 0..10) {
        var j = 0
        whileLabel1@ while(try {j++} finally {} <i) {
            n++;
            if (j==2)
                continue@forLabel1
        }
    }
    assertEquals(19, n)

    n = 0
    forLabel2@ for(i in 0..10) {
        var j = 0
        whileLabel2@ while(j++<i) {
            n++;
            if (j==2)
                break@forLabel2
        }
    }
    assertEquals(3, n)

    n = 0
    forLabel3@ for(i in 0..10) {
        var j = 0
        whileLabel3@ while(j++<i) {
            n++;
            if (j==2)
                break@whileLabel3
        }
    }
    assertEquals(19, n)

    return "OK"
}