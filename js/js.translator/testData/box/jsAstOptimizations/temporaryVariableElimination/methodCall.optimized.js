var log = "";


function init() {
    log = "";
}

function foo_(n) {
    log += "{" + n + "}";
    return n;
}

function test1() {
    var foo = foo_;
    init();

    return foo(foo(1) + foo(2));
}

function test2() {
    var foo = foo_;
    init();

    var $tmp2 = foo(2);

    return foo(foo(1) + $tmp2);
}

function test3() {
    var foo = foo_;
    init();

    return foo(foo(1) + foo(2) + foo(foo(3)));
}

function test4() {
    var foo = foo_;
    init();

    var $tmp1 = foo(1);
    var $tmp2 = foo(2);
    var $tmp3 = foo(3);

    return foo(foo($tmp1) + $tmp2 + $tmp3);
}

function test5() {
    var foo = foo_;
    init();

    var $tmp1 = foo(1);
    var $tmp2 = foo(2);
    var $tmp3 = foo(3);
    foo(4);

    return $tmp1 + $tmp2 + $tmp3;
}

function test6() {
    var foo = foo_;
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