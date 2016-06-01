var global = 0;

function se() {
    return ++global;
}

function se2(value) {
    ++global;
    return value;
}

function test1() {
    se("a1"); se("a2"); se("a3"); se("a4");
    se("b1"); se("b2"); se("b3");
    se("c1"); se("c2");
    se("d1"); se("d2");
    se("e1"); se("e2");
    se("f1"); se("f2"); se("f3"); se("f4");
    se("g1_1"); se("g1_2"); se("g2_1"); se("g2_2");
    se("g3_1"); se("g3_2"); se("g4_1"); se("g4_2");
    se("h1_1"); se("h1_2"); se("h2_1"); se("h2_2");
    se("h3_1"); se("h3_2"); se("h4_1"); se("h4_2");
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
