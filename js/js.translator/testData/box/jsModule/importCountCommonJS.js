$kotlin_test_internal$.beginModule();
module.exports = function(param) {
    switch (typeof param) {
        case "number":
            return "a";
        case "string":
            return "b";
        default:
            return "c";
    }
};
$kotlin_test_internal$.endModule("lib");