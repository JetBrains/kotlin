$kotlin_test_internal$.beginModule();

module.exports = function box() {
    var TableDriver = require("main").api.TableDriver;
    var tableDriver = new TableDriver();

    return {
        value: tableDriver.foo(),
        private: TableDriver.PrivateTable,
        public: TableDriver.PublicTable,
    };
};

$kotlin_test_internal$.endModule("lib");
