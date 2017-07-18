var Tester = require('./test-result-checker');
var expectedOutcomes = require('./expected-outcomes');

module.exports = function (results) {
    var tester = new Tester(expectedOutcomes);
    var testResults = results.testResults[0].testResults;
    for (var i = 0; i < testResults.length; i++) {
        var tr = testResults[i];

        var name = tr.fullName.trim();
        if (tr.status === 'passed') {
            tester.passed(name);
        }
        else if (tr.status === 'failed') {
            tester.failed(name);
        }
        else {
            tester.pending(name);
        }
    }
    tester.end();
};