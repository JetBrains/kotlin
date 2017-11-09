var test = require('tape');
var path = require('path');

var Tester = require('./test-result-checker');

// Tape doesn't report pending tests.
// See https://github.com/substack/tape/pull/197 and https://github.com/substack/tape/issues/90
var full = require('./expected-outcomes');
var noPending = {};
for (var name in full) {
    var result = full[name];
    if (result !== 'pending') {
        noPending[name] = result;
    }
}

var tester = new Tester(noPending);

process.on('exit', function () {
    tester.end();
});

var stream = test.createStream({objectMode: true});

var nameStack = [];
var passed;
var shouldSkip;

stream.on('data', function (row) {
    if (row.type === 'test') {
        var name = row.name === '(anonymous)' ? ' ' : row.name;
        nameStack.push(name);
        passed = true;
        shouldSkip = false;
    }
    else if (row.type === 'end') {
        if (!shouldSkip) {
            var name = nameStack.join(' ').trim();
            if (passed) {
                tester.passed(name);
            }
            else {
                tester.failed(name);
            }
        }
        shouldSkip = true;
        nameStack.pop();
    }
    else if (row.type === 'assert') {
        if (nameStack.length < 2 || row.name === 'fake suite assert') {
            shouldSkip = true;
        }
        if (!row.ok) {
            passed = false;
        }
    }
});

process.argv.slice(2).forEach(function (file) {
    require(path.resolve(file));
});
