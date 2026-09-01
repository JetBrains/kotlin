function foo(x) {
    return x;
}

function box() {
    var $tmp1 = 1;
    var $tmp2 = foo(2);

    var result = $tmp2 + $tmp1;
    if (result != 3) return "fail: " + result;
    
    return "OK";
}