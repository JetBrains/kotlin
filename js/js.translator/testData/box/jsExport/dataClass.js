$kotlin_test_internal$.beginModule();

module.exports = function() {
    var Point = require("JS_TESTS").api.Point;
    var p = new Point(3, 7);

    return {
        "copy00": p.copy().toString(),
        "copy01": p.copy(undefined, 11).toString(),
        "copy10": p.copy(15).toString(),
        "copy11": p.copy(13, 11).toString(),
        "component1": p.component1(),
        "component2": p.component2()
    };
};

$kotlin_test_internal$.endModule("lib");

