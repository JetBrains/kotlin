// EXPECTED_REACHABLE_NODES: 1288
package test

var a = 0

// CHECK_FUNCTION_EXISTS: get_p1 TARGET_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=get_p1 scope=box TARGET_BACKENDS=JS
// CHECK_FUNCTION_EXISTS: set_p1 TARGET_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=set_p1 scope=box TARGET_BACKENDS=JS
// CHECK_FUNCTION_EXISTS: _get_p1__1413126122 IGNORED_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=_get_p1_ scope=box IGNORED_BACKENDS=JS
// CHECK_FUNCTION_EXISTS: _set_p1__3473235702 IGNORED_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=_set_p1__3473235702 scope=box IGNORED_BACKENDS=JS
private inline var p1: Int
    get() = a + 10000
    set(v) {
        a = v + 100
    }

// CHECK_FUNCTION_EXISTS: get_p2 TARGET_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=get_p2 scope=box TARGET_BACKENDS=JS
// CHECK_CALLED_IN_SCOPE: function=set_p2 scope=box TARGET_BACKENDS=JS
// CHECK_FUNCTION_EXISTS: _get_p2__1413126153 IGNORED_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=_get_p2__1413126153 scope=box IGNORED_BACKENDS=JS
// CHECK_CALLED_IN_SCOPE: function=_set_p2__3473235733 scope=box IGNORED_BACKENDS=JS
private var p2: Int
    inline get() = a + 20000
    set(v) {
        a = v + 200
    }

// CHECK_CALLED_IN_SCOPE: function=get_p3 scope=box TARGET_BACKENDS=JS
// CHECK_FUNCTION_EXISTS: set_p3 TARGET_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=set_p3 scope=box TARGET_BACKENDS=JS
// CHECK_CALLED_IN_SCOPE: function=_get_p3__1413126184 scope=box IGNORED_BACKENDS=JS
// CHECK_FUNCTION_EXISTS: _set_p3__3473235764 IGNORED_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=_set_p3__3473235764 scope=box IGNORED_BACKENDS=JS
var p3: Int
    get() = a + 30000
    private inline set(v) {
        a = v + 300
    }

// CHECK_CALLED_IN_SCOPE: function=get_p4 scope=box TARGET_BACKENDS=JS
// CHECK_FUNCTION_EXISTS: set_p4 TARGET_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=set_p4 scope=box TARGET_BACKENDS=JS
// CHECK_CALLED_IN_SCOPE: function=_get_p4__1413126215 scope=box IGNORED_BACKENDS=JS
// CHECK_FUNCTION_EXISTS: _set_p4__3473235795 IGNORED_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=_set_p4__3473235795 scope=box IGNORED_BACKENDS=JS
private var p4: Int
    get() = a + 40000
    inline set(v) {
        a = v + 400
    }

// CHECK_FUNCTION_EXISTS: get_p5 TARGET_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=get_p5 scope=box TARGET_BACKENDS=JS
// CHECK_FUNCTION_EXISTS: set_p5 TARGET_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=set_p5 scope=box TARGET_BACKENDS=JS
// CHECK_FUNCTION_EXISTS: _get_p5__1413126246 IGNORED_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=_get_p5__1413126246 scope=box IGNORED_BACKENDS=JS
// CHECK_FUNCTION_EXISTS: _set_p5__3473235826 IGNORED_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=_set_p5__3473235826 scope=box IGNORED_BACKENDS=JS
private inline var Int.p5: Int
    get() = this * 100 + a + 50000
    set(v) {
        a = this + v + 500
    }

// CHECK_FUNCTION_EXISTS: get_p6 TARGET_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=get_p6 scope=box TARGET_BACKENDS=JS
// CHECK_CALLED_IN_SCOPE: function=set_p6 scope=box TARGET_BACKENDS=JS
// CHECK_FUNCTION_EXISTS: _get_p6__1413126277 IGNORED_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=_get_p6__1413126277 scope=box IGNORED_BACKENDS=JS
// CHECK_CALLED_IN_SCOPE: function=_set_p6__3473235857 scope=box IGNORED_BACKENDS=JS
private var Int.p6: Int
    inline get() = this * 100 + a + 60000
    set(v) {
        a = this + v + 600
    }

// CHECK_CALLED_IN_SCOPE: function=get_p7 scope=box TARGET_BACKENDS=JS
// CHECK_FUNCTION_EXISTS: set_p7 TARGET_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=set_p7 scope=box TARGET_BACKENDS=JS
// CHECK_CALLED_IN_SCOPE: function=_get_p7__1413126308 scope=box IGNORED_BACKENDS=JS
// CHECK_FUNCTION_EXISTS: _set_p7__3473235888 IGNORED_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=_set_p7__3473235888 scope=box IGNORED_BACKENDS=JS
var Int.p7: Int
    get() = this * 100 + a + 70000
    private inline set(v) {
        a = this + v + 700
    }

// CHECK_CALLED_IN_SCOPE: function=get_p8 scope=box TARGET_BACKENDS=JS
// CHECK_FUNCTION_EXISTS: set_p8 TARGET_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=set_p8 scope=box TARGET_BACKENDS=JS
// CHECK_CALLED_IN_SCOPE: function=_get_p8__1413126339 scope=box IGNORED_BACKENDS=JS
// CHECK_FUNCTION_EXISTS: _set_p8__3473235919 IGNORED_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=_set_p8__3473235919 scope=box IGNORED_BACKENDS=JS
private var Int.p8: Int
    get() = this * 100 + a + 80000
    inline set(v) {
        a = this + v + 800
    }


private class A {
    // PROPERTY_NOT_USED: p9
    inline var p9: Int
        get() = a + 90000
        set(v) {
            a = v + 900
        }

    // PROPERTY_NOT_READ_FROM: p10
    var p10: Int
        inline get() = a + 100000
        set(v) {
            a = v + 1000
        }

    // PROPERTY_NOT_WRITTEN_TO: p11
    var p11: Int
        get() = a + 110000
        inline set(v) {
            a = v + 1100
        }

    // CHECK_FUNCTION_EXISTS: get_p12_s8ev3n$ TARGET_BACKENDS=JS
    // CHECK_NOT_CALLED_IN_SCOPE: function=get_p12_s8ev3n$ scope=box TARGET_BACKENDS=JS
    // CHECK_FUNCTION_EXISTS: set_p12_dqglrj$ TARGET_BACKENDS=JS
    // CHECK_NOT_CALLED_IN_SCOPE: function=set_p12_dqglrj$ scope=box TARGET_BACKENDS=JS
    // CHECK_NOT_CALLED_IN_SCOPE: function=_get_p12__quadv7_k$ scope=box IGNORED_BACKENDS=JS
    inline var Int.p12: Int
        get() = this * 100 + a + 120000
        set(v) {
            a = this + v + 1200
        }

    // CHECK_FUNCTION_EXISTS: get_p13_s8ev3n$ TARGET_BACKENDS=JS
    // CHECK_NOT_CALLED_IN_SCOPE: function=get_p13_s8ev3n$ scope=box TARGET_BACKENDS=JS
    // CHECK_CALLED_IN_SCOPE: function=set_p13_dqglrj$ scope=box TARGET_BACKENDS=JS
    var Int.p13: Int
        inline get() = this * 100 + a + 130000
        set(v) {
            a = this + v + 1300
        }

    // CHECK_CALLED_IN_SCOPE: function=get_p14_s8ev3n$ scope=box TARGET_BACKENDS=JS
    // CHECK_FUNCTION_EXISTS: set_p14_dqglrj$ TARGET_BACKENDS=JS
    // CHECK_NOT_CALLED_IN_SCOPE: function=set_p14_dqglrj$ scope=box TARGET_BACKENDS=JS
    var Int.p14: Int
        get() = this * 100 + a + 140000
        inline set(v) {
            a = this + v + 1400
        }
}

// CHECK_FUNCTION_EXISTS: get_p15 TARGET_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=get_p15 scope=box TARGET_BACKENDS=JS
// CHECK_FUNCTION_EXISTS: set_p15 TARGET_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=set_p15 scope=box TARGET_BACKENDS=JS
// CHECK_FUNCTION_EXISTS: _get_p15__857236605 IGNORED_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=_get_p15__857236605 scope=box IGNORED_BACKENDS=JS
// CHECK_FUNCTION_EXISTS: _set_p15__296124145 IGNORED_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=_set_p15__296124145 scope=box IGNORED_BACKENDS=JS
private inline var A.p15: Int
    get() = a + 150000
    set(v) {
        a = v + 1500
    }

// CHECK_FUNCTION_EXISTS: get_p16 TARGET_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=get_p16 scope=box TARGET_BACKENDS=JS
// CHECK_CALLED_IN_SCOPE: function=set_p16 scope=box TARGET_BACKENDS=JS
// CHECK_FUNCTION_EXISTS: _get_p16__857236636 IGNORED_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=_get_p16__857236636 scope=box IGNORED_BACKENDS=JS
// CHECK_CALLED_IN_SCOPE: function=_set_p16__296124176 scope=box IGNORED_BACKENDS=JS
private var A.p16: Int
    inline get() = a + 160000
    set(v) {
        a = v + 1600
    }

// CHECK_CALLED_IN_SCOPE: function=get_p17 scope=box TARGET_BACKENDS=JS
// CHECK_FUNCTION_EXISTS: set_p17 TARGET_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=set_p17 scope=box TARGET_BACKENDS=JS
// CHECK_CALLED_IN_SCOPE: function=_get_p17__857236667 scope=box IGNORED_BACKENDS=JS
// CHECK_FUNCTION_EXISTS: _set_p17__296124207 IGNORED_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=_set_p17__296124207 scope=box IGNORED_BACKENDS=JS
private var A.p17: Int
    get() = a + 170000
    inline set(v) {
        a = v + 1700
    }


fun box(): String {
    p1 = 1
    if (p1 != 10101) return "test1: $p1"
    p2 = 2
    if (p2 != 20202) return "test2: $p2"
    p3 = 3
    if (p3 != 30303) return "test3: $p3"
    p4 = 4
    if (p4 != 40404) return "test4: $p4"

    5000000.p5 = 5
    if (5000000.p5 != 505050505) return "test5: ${5000000.p5}"
    6000000.p6 = 6
    if (6000000.p6 != 606060606) return "test6: ${6000000.p6}"
    7000000.p7 = 7
    if (7000000.p7 != 707070707) return "test7: ${7000000.p7}"
    8000000.p8 = 8
    if (8000000.p8 != 808080808) return "test8: ${8000000.p8}"


    val a = A()

    a.p9 = 9
    if (a.p9 != 90909) return "test9: ${a.p9}"
    a.p10 = 10
    if (a.p10 != 101010) return "test10: ${a.p10}"
    a.p11 = 11
    if (a.p11 != 111111) return "test11: ${a.p11}"

    with (a) {
        12000000.p12 = 12
        if (12000000.p12 != 1212121212) return "test12: ${12000000.p12}"
        13000000.p13 = 13
        if (13000000.p13 != 1313131313) return "test13: ${13000000.p13}"
        14000000.p14 = 14
        if (14000000.p14 != 1414141414) return "test14: ${14000000.p14}"
    }

    a.p15 = 15
    if (a.p15 != 151515) return "test15: ${a.p15}"
    a.p16 = 16
    if (a.p16 != 161616) return "test16: ${a.p16}"
    a.p17 = 17
    if (a.p17 != 171717) return "test17: ${a.p17}"

    return "OK"
}