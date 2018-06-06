var log = "";

function id(x) {
    log += x + ";";
    return x;
}

function box() {
    var $a = id(2);
    var $b = id(3);
    var $c = id(4);
    var $d = id(5);

    if ($a > $b || $c > $d) return "fail condition";
    if (log !== "2;3;4;5;") return "fail log: " + log;

    return "OK";
}