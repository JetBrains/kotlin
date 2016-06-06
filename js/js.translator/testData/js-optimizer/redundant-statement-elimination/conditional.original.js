var global = 0;

function se(value) {
    ++global;
    return value;
}

function test() {
    var x = true;
    var $a = se(x) ? "foo" : "bar";
    var $b = x ? "foo" : "bar";
    var $c = x ? se("foo") : se("bar");
    var $d = x ? "foo" : se("bar");
    var $e = x ? se("foo") : "bar";
    var $f = se(x) ? se("foo") : se("bar");
}

function box() {
    test();
    if (global != 5) return "fail1: " + global;

    return "OK";
}