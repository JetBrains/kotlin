var $tmp;

function test(a, b, c) {
    $tmp = a + b;
    return $tmp + c;
}

function box() {
    var result = test(2, 3, 4);
    if (result != 9) return "fail1: " + result;
    if ($tmp != 5) return "fail2: " + $tmp; 

    return "OK"
}