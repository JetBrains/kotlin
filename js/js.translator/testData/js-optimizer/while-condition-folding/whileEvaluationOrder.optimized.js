var global = "";

function foo(x) {
    global += x + ";";
    return x;
}

function box() {
    var i = 1;
    var sum = 0;
    while (foo(i) < 10 && foo(sum) <= 30) {
        sum += i;
        i++;
    }

    if (global != "1;0;2;1;3;3;4;6;5;10;6;15;7;21;8;28;9;36;") return "fail1: " + global;
    if (sum != 36) return "fail2: " + sum;

    return "OK"
}