function getArg(x) {
    return x;
}

function test1(o, k) {
    var $tmp1;
    var $tmp2;
    if (o != k) {
        $tmp1 = getArg(k).toUpperCase();
        $tmp2 = getArg(o).toUpperCase();

        var O = getArg($tmp2);
        var K = getArg($tmp1);
        return O + K;
    }
}

function test2(o, k) {
    var $tmp1;
    var $tmp2;
    if (o != k) {
        $tmp1 = getArg(k).toUpperCase();
        $tmp2 = getArg(o).toUpperCase();

        if (o) {
            var O = getArg($tmp2);
            var K = getArg($tmp1);
            return O + K;
        }
    }
}

function test3(o, k) {
    var $tmp1;
    var $tmp2;
    if (o != k) {
        $tmp1 = getArg(k).toUpperCase();

        if (o) {
            $tmp2 = getArg(o).toUpperCase();

            var O = getArg($tmp2);
            var K = getArg($tmp1);
            return O + K;
        }
    }
}

function test4(o, k) {
    var $tmp1;
    var $tmp2;
    if (o != k) {
        $tmp1 = getArg(k).toUpperCase();
        $tmp2 = getArg(o).toUpperCase();
    }
    var O = getArg($tmp2);
    var K = getArg($tmp1);
    return O + K;
}

function test5(ok) {
    var $tmp;
    if (ok) {
        if (ok) {
            if (ok) {
                if (ok) {
                    $tmp = getArg(ok).toUpperCase();
                }
            }
            var OK = getArg($tmp);
        }
    }
    return OK
}

function test6(ok) {
    var $tmp;
    if (ok) {
        if (ok) {
            $tmp = 1
            if (ok) {
                if (ok) {
                    $tmp = getArg(ok).toUpperCase();
                }
                var OK = getArg($tmp);
            }
        }
    }
    return OK
}

function box() {
    if (test1("o", "k") != "OK") {
        return "Fail test1"
    }
    if (test2("o", "k") != "OK") {
        return "Fail test2"
    }
    if (test3("o", "k") != "OK") {
        return "Fail test3"
    }
    if (test4("o", "k") != "OK") {
        return "Fail test4"
    }
    if (test5("ok") != "OK") {
        return "Fail test5"
    }
    if (test6("ok") != "OK") {
        return "Fail test6"
    }
    return "OK"
}
