function f(x) {
    return x;
}

function box() {
    var result1, result2;

    if (f(true)) {
        result1 = "1";
    }
    if (result1 !== "1") return "fail1: " + result1;

    if (f(false)) {
        result2 = "1";
    }
    if (result2 !== void 0) return "fail2: " + result2;

    return "OK";
}