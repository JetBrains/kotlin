$kotlin_test_internal$.beginModule();

module.exports = function() {
    var demoPackage = require("main").demoPackage

    var a1 = demoPackage.a1.ModuleA1Class
    var a2 = demoPackage.a2.moduleA2Function
    var bb = demoPackage.b.moduleBFunction

    return {
     "moduleA1": (new a1()).toString(),
     "moduleA2": a2().toString(),
     "moduleB": bb()
    };
};

$kotlin_test_internal$.endModule("lib");

