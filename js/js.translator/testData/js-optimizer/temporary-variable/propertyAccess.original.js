var log = "";

A = {};
Object.defineProperty(A, "x", {
    get: function() {
        log += "A.x;";
        return 23;
    }
});

function b() {
    log += "b();";
    return 42;
}

function box() {
    var $tmp = A.x;
    var result = b() + ";" + $tmp;

    if (result != "42;23") return "fail1: " + result;
    if (log != "A.x;b();") return "fail2: " + log;

    return "OK";
}