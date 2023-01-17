$kotlin_test_internal$.beginModule();

module.exports = function() {
    var Point = require("main").api.Point;
    var p = new Point(3, 7);

    return {
        "copy00": p.copy().toString(),
        "copy01": p.copy(undefined, 11).toString(),
        "copy10": p.copy(15).toString(),
        "copy11": p.copy(13, 11).toString(),
    };
};

$kotlin_test_internal$.endModule("lib");

