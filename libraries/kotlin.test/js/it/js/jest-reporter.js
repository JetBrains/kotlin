var Tester = require('./test-result-checker');
var expectedOutcomes = require('./expected-outcomes').full;

module.exports = function (results) {
    var tester = new Tester(expectedOutcomes);
    var testResults = results.testResults[0].testResults;
    for (var i = 0; i < testResults.length; i++) {
        var tr = testResults[i];


        if (tr.status === 'passed') {
            tester.passed(tr.fullName);
        }
        else if (tr.status === 'failed') {
            tester.failed(tr.fullName);
        }
        else {
            tester.pending(tr.fullName);
        }
    }
    tester.end();
};