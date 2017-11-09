var Tester = require('./test-result-checker');
var tester = new Tester(require('./expected-outcomes'));

process.on('exit', function() {
    tester.end();
});

jasmine.getEnv().addReporter({
    specDone: function(result) {
        var status = result.status;
        var name = result.fullName.trim();
        if (status === 'passed') {
            tester.passed(name);
        }
        else if (status === 'failed') {
            tester.failed(name);
        }
        else {
            tester.pending(name);
        }
    }
});