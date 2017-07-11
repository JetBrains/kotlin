var Tester = function(testMap) {
    this._testMap = testMap;

    this._testCount = {};

    this._passed = 0;
    this._total = Object.keys(testMap).length;

    this._errors = []
};

Tester.prototype._check = function(name, result) {
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

Tester.prototype.passed = function(name) {
    this._check(name, 'pass');
};

Tester.prototype.failed = function(name) {
    this._check(name, 'fail');
};

Tester.prototype.pending = function(name) {
    this._check(name, 'pending');
};

Tester.prototype.printResult = function() {
    console.log("Passed " + this._passed + " out of " + this._total);
    for (var i = 0; i < this._errors.length; ++i) {
        console.log(this._errors[i]);
    }
};

Tester.prototype.exitCode = function() {
    return this._errors.length;
};

Tester.prototype.end = function() {
    this.printResult();
    process.exitCode = this.exitCode();
    process.exit();
};

module.exports = Tester;