$kotlin_test_internal$.beginModule();

module.exports = function() {
    var A = require("main").api.A;
    var B = require("main").api.B;

    return {
        "res": (new A().ping()) + (new B().pong())
    };
};

$kotlin_test_internal$.endModule("lib");

