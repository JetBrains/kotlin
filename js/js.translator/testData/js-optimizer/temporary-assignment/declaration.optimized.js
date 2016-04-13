function test(n) {
    var result;
    if (n >= 0) {
        result = n;
    }
    else {
        result = -n;
    }
    return result;
}

function box() {
    var result = test(20);
    if (result != 20) return "fail1: " + result;

    result = test(-20);
    if (result != 20) return "fail2: " + result;

    return "OK"
}