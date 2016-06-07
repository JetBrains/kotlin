function f(x) {
    return x;
}

function testCatch1() {
    var result;
    try {
        result = f("testCatch1");
        throw new Error();
    } catch (e) {
    }
    return result;
}

function testCatch2() {
    var result;
    try {
        throw new Error();
    } catch (e) {
    }
    return result;
}

function testFinally() {
    var result;
    try {
        result = f("testFinally");
    } finally {
    }
    return result;
}

function testOuter() {
    var result;
    try {
        result = f("testOuter");
    } finally {
        f("23")
    }
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