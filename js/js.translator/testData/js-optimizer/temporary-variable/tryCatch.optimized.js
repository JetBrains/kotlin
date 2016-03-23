function foo() {
    throw "foo";
}

function bar() {
    var $tmp = foo();
    try {
        return "result: " + $tmp;
    } catch (e) {
        return "error";
    }
}

function box() {
    try {
        bar()
    } catch (e) {
        if (e == "foo") {
            return "OK"
        }
        return e;
    }
    return "Exception expected";
}