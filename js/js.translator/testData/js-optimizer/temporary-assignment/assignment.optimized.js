function test(n) {
    var result;
    var $tmp;
    if (n >= 0) {
        $tmp = n;
    } else {
        $tmp = -n;
    }
    result = $tmp;
    return result;
}

function box() {
    var result = test(20);
    if (result != 20) return "fail1: " + result;

    result = test(-20);
    if (result != 20) return "fail2: " + result;

    return "OK"
}
