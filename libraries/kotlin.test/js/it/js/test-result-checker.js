var fs = require('fs');

var Tester = function (testMap, frameworkName) {
    this._testMap = testMap;

    this._isTeamCity = 'TEAMCITY_VERSION' in process.env;
    this._frameworkName = frameworkName;
    this._testStartDate = new Date();

    this._testCount = {};

    this._passed = 0;
    this._total = Object.keys(testMap).length;

    this._errors = []
};

Tester.prototype._check = function (name, result) {
    var count = this._testCount[name] | 0;
    this._testCount = count + 1;
    if (count === 1) {
        this._errors.push('Duplicate test: "' + name + '"');
        return;
    }

    var expected = this._testMap[name];
    if (!expected) {
        this._errors.push('Unexpected test: "' + name + '"');
        return;
    }

    if (result !== expected) {
        this._errors.push('Unexpected test: "' + name + '"');
        return;
    }

    this._passed++;
};

Tester.prototype.passed = function (name) {
    this._check(name, 'pass');
};

Tester.prototype.failed = function (name) {
    this._check(name, 'fail');
};

Tester.prototype.pending = function (name) {
    this._check(name, 'pending');
};

Tester.prototype.hasErrors = function() {
    return this._passed !== this._total || this._errors.length !== 0;
};

Tester.prototype.printResult = function () {
    var message = 'Passed ' + this._passed + ' out of ' + this._total + '\n';
    for (var i = 0; i < this._errors.length; ++i) {
        message += this._errors[i] + '\n';
    }

    if (this._isTeamCity) {
        var testName = 'kotlin-test + ' + this._frameworkName + ' integration test';
        var escapedMessage = message.replace(/\n/g, '|n').replace(/\r/g, '|r');
        message = "##teamcity[testStarted name='" + testName + "' captureStandardOutput='true']\n";
        if (this.hasErrors()) {
            message += "##teamcity[testFailed name='" + testName + "' message='has errors' details='" + escapedMessage + "']\n";
        }
        message +="##teamcity[testFinished name='" + testName + "' duration='" + (new Date() - this._testStartDate) + "']\n";
    }

    fs.writeFileSync('build/tc-' + this._frameworkName + '.log', message);
};

Tester.prototype.exitCode = function () {
    return this._isTeamCity || !this.hasErrors() ? 0 : 1;
};

Tester.prototype.end = function () {
    this.printResult();
    process.exitCode = this.exitCode();
    process.exit();
};

module.exports = Tester;