function f(x) {
    return x;
}

function testRegular() {
    var $a, $b, $c, $d;
    $a = f("testRegular");
    $b = $a;
    $c = $b;
    $d = $c;
    var result = $d;
    return result;
}

function testIrregular() {
    var $a, $b, $d;
    $a = f("testIrregular");
    $b = $a;
    var $c = $b;
    $d = $c;
    var result = $d;
    return result;
}

function testDoubleUse1() {
    var $a, $b, $c, $d;
    $a = f("testDoubleUse1");
    $b = $a;
    $c = $b;
    $d = $c;
    var result = $d;
    f($b);
    return result;
}

function testDoubleUse2() {
    var $a, $b, $c, $d;
    $a = f("testDoubleUse2");
    $b = $a;
    $c = $b;
    $d = $c;
    var result = $d;
    f($d);
    return result;
}

function testDoubleUse3() {
    var $a, $b, $c, $d;
    $a = f("testDoubleUse3");
    $b = $a;
    $c = $b;
    $d = $c;
    var result = $d;
    f($a);
    return result;
}

function testCircular() {
    var $a, $b, $c, $d;
    $a = f("testCircular");
    $b = $a;
    $c = $b;
    $d = $c;
    $b = $d;
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