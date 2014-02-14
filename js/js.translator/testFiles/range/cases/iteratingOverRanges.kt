package foo

import js.*

fun box(): Boolean {

    var d = 0
    for (i in 1..6) {
        d += i
    }
    if (d != 21) return false;

    for (x in 100..199) {
        d += 1
    }
    if (d != 121) {
        return false;
    }

    for (y in 1..1) {
        d = 100
    }

    if (d != 100) {
        return false;
    }

    for (c in 100..100) {
        d += 1;
    }

    if (d != 101) {
        return false;
    }
    return true

}
