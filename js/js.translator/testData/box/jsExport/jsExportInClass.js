$kotlin_test_internal$.beginModule();

module.exports = function() {
    var main = require("main")

    var base = main.api.getBase();
    if (base.getSize() != null) return { res: "fail: cause Derived::size is not null" };

    var exportedDerived = new main.api.ExportedDerived();
    if (exportedDerived.getSize() != null) return { res: "fail: cause ExportedDerived::size is not null" };

    var A = main.api.A;
    var B = main.api.B;

    return {
        "res": (new A().ping()) + (new B().pong())
    };
};

$kotlin_test_internal$.endModule("lib");

