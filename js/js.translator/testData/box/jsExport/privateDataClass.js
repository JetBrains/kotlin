$kotlin_test_internal$.beginModule();

module.exports = function box() {
    var tableDriver = require("main").api;

    return {
        value: tableDriver.foo(),
        private: tableDriver.PrivateTable,
        public: tableDriver.PublicTable,
    };
};

$kotlin_test_internal$.endModule("lib");
