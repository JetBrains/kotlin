$kotlin_test_internal$.beginModule();

module.exports = function() {
    var A = require("JS_TESTS").api.A;
    var B = require("JS_TESTS").api.B;

    return {
        "res": (new A().ping()) + (new B().pong())
    };
};

$kotlin_test_internal$.endModule("lib");

