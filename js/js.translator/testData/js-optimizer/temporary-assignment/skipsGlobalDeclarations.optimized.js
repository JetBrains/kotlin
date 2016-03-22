var $tmp;

function test(n) {
    if (n >= 0) {
        $tmp = n;
    }
    else {
        $tmp = -n;
    }
    var result = $tmp;
    return result;
}

function box() {
    var result = test(20);
    if (result != 20) return "fail1: " + result;

    result = test(-20);
    if (result != 20) return "fail2: " + result;

    if ($tmp != 20) return "fail3: " + result;

    return "OK"
}