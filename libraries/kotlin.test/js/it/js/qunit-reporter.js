var Tester = require('./test-result-checker');
var tester = new Tester(require('./expected-outcomes'));

QUnit.testDone(function (details) {
    var testName = (details.module.replace('> ', '') + ' ' + details.name).trim();
    if (details.skipped) {
        tester.pending(testName);
    }
    else if (!details.failed) {
        tester.passed(testName);
    }
    else {
        tester.failed(testName);
    }
});

QUnit.done(function (details) {
    tester.printResult();
    details.failed = tester.exitCode();
});

