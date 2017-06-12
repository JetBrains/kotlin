// EXPECTED_REACHABLE_NODES: 896
package foo


val d = { a: Int -> a + 1 }
val p = { a: Int -> a * 3 }

val list = ArrayList<Function1<Int, Int>>();

fun chain(start: Int): Int {
    var res = start;
    for (func in list) {
        res = (func)(res);
    }
    return res;
}

fun box(): String {
    if (chain(0) != 0) {
        return "fail1"
    }
    list.add(d);
    if (list.get(0)(0) != 1) {
        return "fail2"
    }
    list.add(p);
    if (list.get(1)(10) != 30) {
        return "fail3"
    }
    if (chain(0) != 3) {
        return "fail4"
    }
    list.add({ it * it });
    list.add({ it - 100 });
    if (chain(2) != -19) {
        return "fail5"
    }
    if (({ a: Int -> a * a }(3)) != 9) {
        return "fail7"
    }
    return "OK"
}