function test(n) {
    /*synthetic*/ if (n >= 0) {
        return n;
    }
    else {
        return -n;
    }
}

function box() {
    var result = test(20);
    if (result != 20) return "fail1: " + result;

    result = test(-20);
    if (result != 20) return "fail2: " + result;

    return "OK"
}