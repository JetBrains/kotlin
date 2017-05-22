var Tester = require('./test-result-checker');
var tester = new Tester(require('./expected-outcomes').full);

process.on('exit', function() {
    tester.end();
});

jasmine.getEnv().addReporter({
    specDone: function(result) {
        var status = result.status;
        if (status === 'passed') {
            tester.passed(result.fullName);
        }
        else if (status === 'failed') {
            tester.failed(result.fullName);
        }
        else {
            tester.pending(result.fullName);
        }
    }
});