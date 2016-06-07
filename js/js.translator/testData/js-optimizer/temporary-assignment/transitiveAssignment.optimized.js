function test(param) {
    var result;
    if (param > 0) {
        result = param;
    } else {
        result = -param;
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