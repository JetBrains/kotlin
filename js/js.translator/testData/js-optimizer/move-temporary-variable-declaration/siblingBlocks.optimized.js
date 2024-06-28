function getArg(x) {
    return x;
}

function test1(o, k) {
    var $tmp1;
    var $tmp2;
    if (o != k) {
        $tmp1 = getArg(k).toUpperCase();
        $tmp2 = getArg(o).toUpperCase();
    } else {
        $tmp1 = getArg(k)
        $tmp2 = getArg(o)
    }

    var O = getArg($tmp2);
    var K = getArg($tmp1);
    return O + K;
}

function test2(o, k) {
    var $tmp1;
    var $tmp2;
    if (o != k) {
        if (o) {
            $tmp1 = getArg(k).toUpperCase();
            $tmp2 = getArg(o).toUpperCase();
        } else {
            $tmp1 = getArg(k)
            $tmp2 = getArg(o)
        }
        var O = getArg($tmp2);
        var K = getArg($tmp1);
    }

    return O + K;
}

function test3(o, k) {
    if (o != k) {
        var $tmp1 = getArg(k).toUpperCase();
        var $tmp2 = getArg(k).toUpperCase();
        if (o) {
            $tmp1 = getArg(k).toUpperCase();
            $tmp2 = getArg(o).toUpperCase();
        } else {
            $tmp1 = getArg(k)
            $tmp2 = getArg(o)
        }
        var O = getArg($tmp2);
        var K = getArg($tmp1);
    }

    return O + K;
}

function test4(ok) {
    if (ok) {
        var $tmp1 = getArg(ok).toUpperCase();
        if (ok) {
            if (ok) {
                getArg($tmp1)
            }
        } else {
            getArg($tmp1)
        }
        var OK = getArg($tmp1);
    }

    return OK;
}

function test5(ok) {
    if (ok) {
        var $tmp1 = 0
        if (ok) {
            $tmp1 = 1
            if (ok) {
                getArg($tmp1)
            } else {
                $tmp1 = 2
            }
            $tmp1 = getArg(ok).toUpperCase();
        } else {
            getArg($tmp1)
        }
        var OK = getArg($tmp1);
    }
    return OK;
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
    if (test4("OK") != "OK") {
        return "Fail test4"
    }
    if (test5("OK") != "OK") {
        return "Fail test5"
    }
    return "OK"
}
