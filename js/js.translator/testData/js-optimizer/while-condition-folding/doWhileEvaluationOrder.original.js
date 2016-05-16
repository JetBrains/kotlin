var global = "";

function foo(x) {
    global += x + ";";
    return x;
}

function box() {
    var i = 1;
    var sum = 0;
    do {
        sum += i;
        i++;
        if (foo(i) >= 10) {
            break;
        }
        if (foo(sum) > 30) {
            break;
        }
    } while (true);

    if (global != "2;1;3;3;4;6;5;10;6;15;7;21;8;28;9;36;") return "fail1: " + global;
    if (sum != 36) return "fail2: " + sum;

    return "OK"
}