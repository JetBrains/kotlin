function f(x) {
    return x;
}

function testCatch1() {
    var $tmp;
    var result;
    try {
        $tmp = f("testCatch1");
        throw new Error();
    } catch (e) {
        result = $tmp;
    }
    return result;
}

function testCatch2() {
    var $tmp;
    var result;
    try {
        throw new Error();
    } catch (e) {
        result = $tmp;
    }
    return result;
}

function testFinally() {
    var result;
    result = f("testFinally");
    return result;
}

function testOuter() {
    var $tmp;
    var result;
    try {
        $tmp = f("testOuter");
    } finally {
        f("23")
    }
    result = $tmp;
    return result;
}

function box() {
    var result = testCatch1();
    if (result !== "testCatch1") return "failCatch1: " + result;

    result = testCatch2();
    if (result !== void 0) return "failCatch2: " + result;

    result = testFinally();
    if (result !== "testFinally") return "failFinally: " + result;

    result = testOuter();
    if (result !== "testOuter") return "testOuter: " + result;

    return "OK";
}
