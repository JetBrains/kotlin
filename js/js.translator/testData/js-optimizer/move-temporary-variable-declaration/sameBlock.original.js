function getArg(x) {
    return x;
}

function test(o, k) {
    var $tmp1;
    var $tmp2;
    $tmp1 = getArg(k).toUpperCase();
    $tmp2 = getArg(o).toUpperCase();

    var O = getArg($tmp2);
    var K = getArg($tmp1);
    return O + K;
}

function box() {
    return test("o", "k");
}
