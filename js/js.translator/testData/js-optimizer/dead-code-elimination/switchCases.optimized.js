function test(x) {
    var log = "";
    switch (x) {
        case 0:
            log += "0;";
            break;
        case 1:
            return "one";
        case 2:
            log += "2;";
        case 3:
            log += "3;";
            break;
        default:
            if (x == 4) {
                log += "four;";
                break;
            }
            else {
                return "default";
            }
    }
    return log;
}

function box() {
    if (test(0) != "0;") return "fail1";
    if (test(1) != "one") return "fail2";
    if (test(2) != "2;3;") return "fail3";
    if (test(3) != "3;") return "fail4";
    if (test(4) != "four;") return "fail5";
    if (test(5) != "default") return "fail5";

    return "OK";
}