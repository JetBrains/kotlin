$kotlin_test_internal$.beginModule();

module.exports = function box() {
    var array = require("main").array;
    array.push("Kotlin")
    array.push("JS")
    array.push("OK")

    return {
        value: array.pop()
    };
};

$kotlin_test_internal$.endModule("lib");
