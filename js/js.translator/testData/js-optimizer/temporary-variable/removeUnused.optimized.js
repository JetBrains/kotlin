var global = 0;

function test1() {
    global++;
    return global;
}

function test2() {
    global++;
    return global;
}

function test3() {
    global++;
    var $b = global--;
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