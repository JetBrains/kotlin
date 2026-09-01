var global = 0;

function test1() {
    var $tmp = global++;
    return global;
}

function test2() {
    var $tmp;
    $tmp = global++;
    return global;
}

function test3() {
    var $a = global++, $b = global--;
    return $b + $b;
}

function box() {
    var result = test1();
    if (result != 1) return "fail1: " + result;

    result = test2();
    if (result != 2) return "fail2: " + result;

    result = test3();
    if (result != 6) return "fail3: " + result;

    return "OK"
}