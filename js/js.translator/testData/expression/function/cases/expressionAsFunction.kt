package foo

import java.util.*;

val d = {(a: Int) -> a + 1 }
val p = {(a: Int) -> a * 3 }

val list = ArrayList<Function1<Int, Int>>();

fun chain(start: Int): Int {
    var res = start;
    for (func in list) {
        res = (func)(res);
    }
    return res;
}

fun box(): Boolean {
    if (chain(0) != 0) {
        return false;
    }
    list.add(d);
    if (list.get(0)(0) != 1) {
        return false;
    }
    list.add(p);
    if (list.get(1)(10) != 30) {
        return false;
    }
    if (chain(0) != 3) {
        return false;
    }
    list.add({ it * it });
    list.add({ it - 100 });
    if (chain(2) != -19) {
        return false;
    }
    if (({(a: Int) -> a * a }(3)) != 9) {
        return false;
    }
    return true;
}