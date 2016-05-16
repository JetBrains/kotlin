var global = "";

function foo(x) {
    global += x;
    return x;
}

function box() {
    var i = 0;
    var j;
    do {
        ++i;
        global += ";";
        for (j = 0; j < 2; ++j) {
            if (j == 1) {
                continue;
            }
            global += "-";
        }
        for (k in { a: 2, b: 3 }) {
            if (k != "a") {
                continue;
            }
            global += "@";
        }
        j = 0;
        while (j++ < 2) {
            if (j == 1) {
                continue;
            }
            global += "$";
        }
        j = 0;
        do {
            if (j == 1) {
                continue;
            }
            global += "#";
        } while (j++ < 2);
        if (foo(i) >= 3) {
            break;
        }
    } while (true);

    if (global != ";-@$##1;-@$##2;-@$##3") return "fail: " + global;

    return "OK"
}