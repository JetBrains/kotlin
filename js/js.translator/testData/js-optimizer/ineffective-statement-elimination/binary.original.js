var global = 0;

function se() {
    return ++global;
}

function se2(value) {
    ++global;
    return value;
}

function test1() {
    var $a = (se("a1") - se("a2") + se("a3")) * se("a4");
    var $b = se("b1") % se("b2") / se("b3");
    var $c = se("c1") >> se("c2");
    var $d = se("d1") << se("d2");
    var $e = se("e1") >>> se("e2");
    var $f = se("f1") | se("f2") & se("f3") ^ se("f4");
    var $g1 = se("g1_1") == se("g1_2");
    var $g2 = se("g2_1") === se("g2_2");
    var $g3 = se("g3_1") != se("g3_2");
    var $g4 = se("g4_1") !== se("g4_2");
    var $h1 = se("h1_1") > se("h1_2");
    var $h2 = se("h2_1") >= se("h2_2");
    var $h3 = se("h3_1") < se("h3_2");
    var $h4 = se("h4_1") <= se("h4_2");
}

function test2() {
    try {
        var $a = se() in se();
        return "fail2a: `in` should not be removed"
    }
    catch (e) {
        // Do nothing
    }

    try {
        var $b = se() instanceof se();
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

    var $a1 = se2(true) && x;
    var $a2 = x && se2(true);
    var $a3 = se2(true) && se2(true);
    var $a4 = x && x;

    var $b1 = se2(false) || y;
    var $b2 = y || se2(false);
    var $b3 = se2(false) || se2(false);
    var $b4 = y || y;
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
