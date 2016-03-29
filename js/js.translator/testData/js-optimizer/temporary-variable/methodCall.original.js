var log = "";


function init() {
    log = "";
}

function foo(n) {
    log += "{" + n + "}";
    return n;
}

function test1() {
    init();

    var $tmp1 = foo(1);
    var $tmp2 = foo(2);

    return foo($tmp1 + $tmp2);
}

function test2() {
    init();

    var $tmp2 = foo(2);
    var $tmp1 = foo(1);

    return foo($tmp1 + $tmp2);
}

function test3() {
    init();

    var $tmp1 = foo(1);
    var $tmp2 = foo(2);
    var $tmp3 = foo(3);

    return foo($tmp1 + $tmp2 + foo($tmp3));
}

function test4() {
    init();

    var $tmp1 = foo(1);
    var $tmp2 = foo(2);
    var $tmp3 = foo(3);

    return foo(foo($tmp1) + $tmp2 + $tmp3);
}

function test5() {
    init();

    var $tmp1 = foo(1);
    var $tmp2 = foo(2);
    var $tmp3 = foo(3);
    foo(4);

    return $tmp1 + $tmp2 + $tmp3;
}

function test6() {
    init();

    var $tmp = foo(1);
    return foo(2) + $tmp;
}

function box() {
    var result = test1();
    if (result != 3) return "fail1a: " + result;
    if (log != "{1}{2}{3}") return "fail1b: " + log;

    result = test2();
    if (result != 3) return "fail2a: " + result;
    if (log != "{2}{1}{3}") return "fail2b: " + log;

    result = test3();
    if (result != 6) return "fail3a: " + result;
    if (log != "{1}{2}{3}{3}{6}") return "fail3b: " + log;

    result = test4();
    if (result != 6) return "fail4a: " + result;
    if (log != "{1}{2}{3}{1}{6}") return "fail4b: " + log;

    result = test5();
    if (result != 6) return "fail5a: " + result;
    if (log != "{1}{2}{3}{4}") return "fail5b: " + log;

    result = test6();
    if (result != 3) return "fail6a: " + result;
    if (log != "{1}{2}") return "fail6b: " + result;

    return "OK";
}