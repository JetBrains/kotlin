var global = 0;

function se() {
    return ++global;
}

function se2(value) {
    ++global;
    return value;
}

function test1() {
    var $a = (se() - se() + se()) * se();
    var $b = se() % se() / se();
    var $c = se() >> se();
    var $d = se() << se();
    var $e = se() >>> se();
    var $f = se() | se() & se() ^ se();
    var $g1 = se() == se();
    var $g2 = se() === se();
    var $g3 = se() != se();
    var $g4 = se() !== se();
    var $h1 = se() > se();
    var $h2 = se() >= se();
    var $h3 = se() < se();
    var $h4 = se() <= se();
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
