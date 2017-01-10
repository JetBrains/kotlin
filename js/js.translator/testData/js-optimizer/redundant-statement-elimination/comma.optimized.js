var global = "";

function log(x) {
    global += x + ";"
}

function box() {
    var $x = log(1);
    var result = (log(2), log(3));
    if (global != "1;2;3;") return "fail: " + global;
    return "OK";
}