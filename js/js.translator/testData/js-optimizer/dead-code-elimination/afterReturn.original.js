function test1(x) {
    var $tmp0_elvis_lhs = x;
    var $tmp;
    if ($tmp0_elvis_lhs == null) {
        var $tmp$ret$0;
        return 'OK';
        $tmp = $tmp$ret$0;
    } else {
        $tmp = $tmp0_elvis_lhs;
    }
    var z = $tmp;
    return 'Fail 1: ' + z;
}

function test2(x) {
    return function () {
        var $tmp0_elvis_lhs = x;
        var $tmp;
        if ($tmp0_elvis_lhs == null) {
            var $tmp$ret$0;
            return 'OK';
            $tmp = $tmp$ret$0;
        } else {
            $tmp = $tmp0_elvis_lhs;
        }
        var z = $tmp;
        return 'Fail 1: ' + z;
    }
}

function box() {
    if (test1(null) != "OK") {
        return "Fail test1";
    }
    if (test2(null)() != "OK") {
        return "Fail test2";
    }
    return "OK"
}
