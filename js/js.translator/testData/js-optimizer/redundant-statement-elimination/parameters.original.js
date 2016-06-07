var log = "";

function se(x) {
    log += x;
    return x;
}

function test(param) {
    var $tmp = param + se("test:" + param);
}

function box() {
    test(23);
    if (log != "test:23") return "fail: " + log;
    return "OK";
}