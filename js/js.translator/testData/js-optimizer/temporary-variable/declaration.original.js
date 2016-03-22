function test(a, b, c) {
    var $tmp = a + b;
    return $tmp + c;
}

function box() {
    var result = test(2, 3, 4);
    if (result != 9) return "fail: " + result;

    return "OK"
}