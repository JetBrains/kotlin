function test(param) {
    var $a;
    var result;
    if (param > 0) {
        var $b = param;
        $a = $b;
    } else {
        var $c = -param;
        $a = $c;
    }
    result = $a;
    return result;
}

function box() {
    var result = test(20);
    if (result != 20) return "fail1: " + result;

    result = test(-20);
    if (result != 20) return "fail2: " + result;

    return "OK"
}