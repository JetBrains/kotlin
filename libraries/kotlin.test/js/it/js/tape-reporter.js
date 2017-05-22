var test = require('tape');
var path = require('path');

var Tester = require('./test-result-checker');
// Tape doesn't report pending tests.
// See https://github.com/substack/tape/pull/197 and https://github.com/substack/tape/issues/90
var tester = new Tester(require('./expected-outcomes').noPending);

process.on('exit', function () {
    tester.end();
});

var stream = test.createStream({objectMode: true});

var nameStack = [];

stream.on('data', function (row) {
    console.log(JSON.stringify(row));
    if (row.type === 'test') {
        nameStack.push(row.name);
    }
    else if (row.type === 'end') {
        nameStack.pop();
    }
    else if (row.type === 'assert') {
        var name = nameStack.join(' ');
        if (row.ok) {
            tester.passed(name);
        }
        else {
            tester.failed(name);
        }
    }
});

process.argv.slice(2).forEach(function (file) {
    require(path.resolve(file));
});
