var mocha = require('mocha');
var Tester = require('./test-result-checker');
var expectedOutcomes = require('./expected-outcomes').full;

module.exports = function (runner) {
    mocha.reporters.Base.call(this, runner);

    var tester = new Tester(expectedOutcomes);

    runner.on('pass', function (test) {
        tester.passed(test.fullTitle());
    });

    runner.on('fail', function (test, err) {
        tester.failed(test.fullTitle());
    });

    runner.on('pending', function (test) {
        tester.pending(test.fullTitle());
    });

    runner.on('end', function () {
        tester.end();
    });
};
