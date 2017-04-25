// EXPECTED_REACHABLE_NODES: 508
// MODULE: lib
// FILE: lib.kt

package test

var a = 0

inline var p1: Int
    get() = a + 10000
    set(v) {
        a = v + 100
    }

var p2: Int
    inline get() = a + 20000
    set(v) {
        a = v + 200
    }

var p3: Int
    get() = a + 30000
    inline set(v) {
        a = v + 300
    }

inline var Int.p4: Int
    get() = this * 100 + a + 40000
    set(v) {
        a = this + v + 400
    }

var Int.p5: Int
    inline get() = this * 100 + a + 50000
    set(v) {
        a = this + v + 500
    }

var Int.p6: Int
    get() = this * 100 + a + 60000
    inline set(v) {
        a = this + v + 600
    }

class A {
    inline var p7: Int
        get() = a + 70000
        set(v) {
            a = v + 700
        }

    var p8: Int
        inline get() = a + 80000
        set(v) {
            a = v + 800
        }

    var p9: Int
        get() = a + 90000
        inline set(v) {
            a = v + 900
        }

    inline var Int.p10: Int
        get() = this * 100 + a + 100000
        set(v) {
            a = this + v + 1000
        }

    var Int.p11: Int
        inline get() = this * 100 + a + 110000
        set(v) {
            a = this + v + 1100
        }

    var Int.p12: Int
        get() = this * 100 + a + 120000
        inline set(v) {
            a = this + v + 1200
        }
}

inline var A.p13: Int
    get() = a + 130000
    set(v) {
        a = v + 1300
    }

var A.p14: Int
    inline get() = a + 140000
    set(v) {
        a = v + 1400
    }

var A.p15: Int
    get() = a + 150000
    inline set(v) {
        a = v + 1500
    }

// MODULE: main(lib)
// FILE: main.kt
// PROPERTY_NOT_USED: p1
// PROPERTY_NOT_READ_FROM: p2
// PROPERTY_NOT_WRITTEN_TO: p3
// CHECK_NOT_CALLED: imported$get_p4
// CHECK_NOT_CALLED: imported$set_p4
// CHECK_NOT_CALLED: imported$get_p5
// CHECK_NOT_CALLED: imported$set_p6
// PROPERTY_NOT_USED: p7
// PROPERTY_NOT_READ_FROM: p8
// PROPERTY_NOT_WRITTEN_TO: p9
// CHECK_NOT_CALLED: imported$A$get_A$p10
// CHECK_NOT_CALLED: imported$A$set_A$p10
// CHECK_NOT_CALLED: imported$A$get_A$p11
// CHECK_NOT_CALLED: imported$A$set_A$p12
// CHECK_NOT_CALLED: imported$get_p13
// CHECK_NOT_CALLED: imported$set_p13
// CHECK_NOT_CALLED: imported$get_p14
// CHECK_NOT_CALLED: imported$set_p15

import test.*

fun box(): String {
    p1 = 1
    if (p1 != 10101) return "test1: $p1"
    p2 = 2
    if (p2 != 20202) return "test2: $p2"
    p3 = 3
    if (p3 != 30303) return "test3: $p3"

    4000000.p4 = 4
    if (4000000.p4 != 404040404) return "test4: ${4000000.p4}"
    5000000.p5 = 5
    if (5000000.p5 != 505050505) return "test5: ${5000000.p5}"
    6000000.p6 = 6
    if (6000000.p6 != 606060606) return "test6: ${6000000.p6}"


    val a = A()

    a.p7 = 7
    if (a.p7 != 70707) return "test7: ${a.p7}"
    a.p8 = 8
    if (a.p8 != 80808) return "test8: ${a.p8}"
    a.p9 = 9
    if (a.p9 != 90909) return "test9: ${a.p9}"

    with (a) {
        10000000.p10 = 10
        if (10000000.p10 != 1010101010) return "test10: ${10000000.p10}"
        11000000.p11 = 11
        if (11000000.p11 != 1111111111) return "test11: ${11000000.p11}"
        12000000.p12 = 12
        if (12000000.p12 != 1212121212) return "test12: ${12000000.p12}"
    }

    a.p13 = 13
    if (a.p13 != 131313) return "test13: ${a.p13}"
    a.p14 = 14
    if (a.p14 != 141414) return "test14: ${a.p14}"
    a.p15 = 15
    if (a.p15 != 151515) return "test15: ${a.p15}"

    return "OK"
}