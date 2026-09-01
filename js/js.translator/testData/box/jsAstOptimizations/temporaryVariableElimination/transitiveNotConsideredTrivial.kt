var global = 1;

function se() {
    return global++;
}

function box() {
    var $a = se();
    var $b = $a;
    var result = $b + $b;
    if (result != 2) return "fail: " + result;

    return "OK";
}