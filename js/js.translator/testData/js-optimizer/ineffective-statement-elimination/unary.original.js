var global = 0;

function se(value) {
    ++global;
    return value;
}

function test1() {
    var $a = +se(23);
    var $b = -se(23);
    var $c = !se(false);
    var $d = ~se(-1);
    var $e = typeof se(23);
    var $f = void se(23);
}

function test2() {
    var $a = ++global;
    var $b = global++;
    var $c = --global;
    var $d = global--;
}

function test3() {
    var obj = { x: 2, y: 3};
    var $tmp = delete obj.x;
    return ("x" in obj) + ":" + ("y" in obj);
}

function box() {
    test1();
    if (global != 6) return "fail1: " + global;

    test2();
    if (global != 6) return "fail2: " + global;

    var result = test3();
    if (result != "false:true") return "fail3: " + result;

    return "OK";
}