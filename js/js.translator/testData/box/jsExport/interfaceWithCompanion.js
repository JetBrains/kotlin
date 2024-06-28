$kotlin_test_internal$.beginModule();

module.exports = function() {
    var { A } = require("main").api

    return {
        "res": A.ok()
    };
};

$kotlin_test_internal$.endModule("lib");

