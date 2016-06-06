var global = 0;

function se(value) {
    ++global;
    return value;
}

function test1() {
    se(23);
    se(23);
    se(false);
    se(-1);
    se(23);
    se(23);
}

function test2() {
    ++global;
    global++;
    --global;
    global--;
}

function test3() {
    var obj = { x: 2, y: 3};
    delete obj.x;
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