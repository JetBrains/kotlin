var Tester = require('./test-result-checker');

var full = require('./expected-outcomes');
var allAsyncPass = {};
for (var name in full) {
    allAsyncPass[name] = name.startsWith('AsyncTest ') ? 'pass' : full[name];
}

var tester = new Tester(allAsyncPass, 'qunit');

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

