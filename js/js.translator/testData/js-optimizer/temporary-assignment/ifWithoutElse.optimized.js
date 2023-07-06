function f(x) {
    return x;
}

function box() {
    var $a, $b;

    if (f(true)) {
        $a = "1";
    }
    var result1 = $a;
    if (result1 !== "1") return "fail1: " + result1;

    if (f(false)) {
        $b = "1";
    }
    var result2 = $b;
    if (result2 !== void 0) return "fail2: " + result2;

    return "OK";
}
