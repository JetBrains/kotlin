function test(a, b, c) {
    return a + b + c;
}

function box() {
    var result = test(2, 3, 4);
    if (result != 9) return "fail: " + result;

    return "OK"
}