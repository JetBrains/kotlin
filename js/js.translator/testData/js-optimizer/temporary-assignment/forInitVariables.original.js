function test(x) {
    var $tmp;
    if (x > 9) {
        $tmp = 9;
    }
    else {
        $tmp = x;
    }

    var result = x;
    for (var i = $tmp; i < 10; ++i) {
        result += i;
    }

    return result;
}

function box() {
    var result = test(11);
    if (result != 20) return "fail1: " + result;

    result = test(8);
    if (result != 25) return "fail2: " + result;

    return "OK";
}