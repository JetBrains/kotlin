function getArg(x) {
    return x;
}

function test1(ok) {
    var $tmp
    getArg($tmp)

    $tmp = getArg(ok).toUpperCase();
    return $tmp
}

function test2(ok) {
    var $tmp
    if ($tmp === undefined) {
        $tmp = getArg(ok).toUpperCase();
    }

    return $tmp
}

function test3(ok) {
    var $tmp
    if (ok) {
        if (ok) {
            getArg($tmp)
            $tmp = getArg(ok).toUpperCase();
        }
    }
    return $tmp
}

function test4(ok) {
    var $tmp
    if (ok) {
        if (ok) {
            getArg($tmp)
        }
        $tmp = getArg(ok).toUpperCase();
    }
    return $tmp
}

function box() {
    if (test1("ok") != "OK") {
        return "Fail test1"
    }
    if (test2("ok") != "OK") {
        return "Fail test2"
    }
    if (test3("ok") != "OK") {
        return "Fail test3"
    }
    if (test4("ok") != "OK") {
        return "Fail test4"
    }
    return "OK"
}
