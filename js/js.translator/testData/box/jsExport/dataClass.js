$kotlin_test_internal$.beginModule();

module.exports = function() {
    var Point = require("JS_TESTS").api.Point;
    var p = new Point(3, 7);

    return {
        "res": p.copy(13, 11).toString()
    };
};

$kotlin_test_internal$.endModule("lib");

