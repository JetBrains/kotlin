function f(x) {
    return x;
}

function testRegular() {
    var result;
    result = f("testRegular");
    return result;
}

function testIrregular() {
    var result;
    result = f("testIrregular");
    return result;
}

function testDoubleUse1() {
    var result;
    var $b;
    $b = f("testDoubleUse1");
    result = $b;
    f($b);
    return result;
}

function testDoubleUse2() {
    var $d;
    $d = f("testDoubleUse2");
    var result = $d;
    f($d);
    return result;
}

function testDoubleUse3() {
    var result;
    var $a;
    $a = f("testDoubleUse3");
    result = $a;
    f($a);
    return result;
}

function testCircular() {
    var $b;
    $b = f("testCircular");
    $b = $b;
    var result = $b;
    return result;
}

function box() {
    var result = testRegular();
    if (result != "testRegular") return "failRegular: " + result;

    result = testIrregular();
    if (result != "testIrregular") return "failIrregular: " + result;

    result = testDoubleUse1();
    if (result != "testDoubleUse1") return "failDoubleUse1: " + result;

    result = testDoubleUse2();
    if (result != "testDoubleUse2") return "failDoubleUse2: " + result;

    result = testDoubleUse3();
    if (result != "testDoubleUse3") return "failDoubleUse3: " + result;

    result = testCircular();
    if (result != "testCircular") return "failCircular: " + result;

    return "OK"
}