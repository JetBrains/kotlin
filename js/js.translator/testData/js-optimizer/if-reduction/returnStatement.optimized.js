function test(n) {
    return n >= 0 ? n : -n;
}

function box() {
    var result = test(20);
    if (result != 20) return "fail1: " + result;

    result = test(-20);
    if (result != 20) return "fail2: " + result;

    return "OK"
}