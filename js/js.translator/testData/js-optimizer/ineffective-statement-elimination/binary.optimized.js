var global = 0;

function se() {
    return ++global;
}

function se2(value) {
    ++global;
    return value;
}

function test1() {
    se(); se(); se(); se(); se();
    se(); se(); se(); se(); se();
    se(); se(); se(); se(); se();
    se(); se(); se(); se(); se();
    se(); se(); se(); se(); se();
    se(); se(); se(); se(); se();
    se(); se(); se();
}

function test2() {
    try {
        se() in se();
        return "fail2a: `in` should not be removed"
    }
    catch (e) {
        // Do nothing
    }

    try {
        se() instanceof se();
        return "fail2b: `instanceof` should not be removed"
    }
    catch (e) {
        // Do nothing
    }

    return "OK"
}

function test3() {
    var x = true;
    var y = false;

    se2(true);
    x && se2(true);
    se2(true) && se2(true);

    se2(false);
    y || se2(false);
    se2(false) || se2(false);
}

function box() {
    test1();
    if (global != 33) return "fail1: " + global;

    var result = test2();
    if (result !== "OK") return result;
    if (global != 37) return "fail2: " + global;

    test3();
    if (global != 45) return "fail3: " + global;

    return "OK";
}
