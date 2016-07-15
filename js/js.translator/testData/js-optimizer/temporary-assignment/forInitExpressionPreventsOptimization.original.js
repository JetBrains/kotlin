function test(x) {
    var $tmp;
    if (x > 9) {
        $tmp = 9;
    }
    else {
        $tmp = x;
    }
    var y = $tmp;

    var result = x;
    for (var i = $tmp; i < 10; ++i) {
        result += i;
    }

    return result + y;
}

function box() {
    var result = test(11);
    if (result != 29) return "fail1: " + result;

    result = test(8);
    if (result != 33) return "fail2: " + result;

    return "OK";
}