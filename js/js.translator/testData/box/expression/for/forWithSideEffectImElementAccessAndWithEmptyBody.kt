// EXPECTED_REACHABLE_NODES: 506
package foo

var log = ""

class T(val id: Int) {
    operator fun component1(): Int {
        log += "($id).component1();"
        return 1
    }
    operator fun component2(): String {
        log += "($id).component2();"
        return "1"
    }
}

class C {
    operator fun iterator(): Iterator<T> = object: Iterator<T> {
        var i = 0
        var data = arrayOf(T(3), T(1), T(2))
        override fun hasNext(): Boolean {
            log += "C.hasNext();"
            return i < data.size
        }

        override fun next(): T {
            log += "C.next();"
            return data[i++]
        }
    }
}

fun box(): String {
    for ((a, b) in arrayOf(T(3), T(1), T(2)));
    assertEquals("(3).component1();(3).component2();(1).component1();(1).component2();(2).component1();(2).component2();", log)


    log = ""
    for ((a, b) in C());
    assertEquals("C.hasNext();C.next();(3).component1();(3).component2();C.hasNext();C.next();" +
                 "(1).component1();(1).component2();C.hasNext();C.next();" +
                 "(2).component1();(2).component2();C.hasNext();", log)

    return "OK"
}

