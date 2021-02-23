$kotlin_test_internal$.beginModule();

module.exports = function() {
    var ping = require("JS_TESTS").ping;
    var Something = require("JS_TESTS").Something;

    return {
        "pingCall": function() {
            return ping(new Something())
        },
    };
};

$kotlin_test_internal$.endModule("lib");
