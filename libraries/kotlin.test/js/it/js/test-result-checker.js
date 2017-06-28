var Tester = function(testMap) {
    this._testMap = testMap;

    this._testCount = {};

    this._passed = 0;
    this._total = Object.keys(testMap).length;
};

Tester.prototype._check = function(name, result) {
    var count = this._testCount[name] | 0;
    this._testCount = count + 1;
    if (count === 1) {
        throw new Error('Duplicate test: "' + name + '"');
    }

    var expected = this._testMap[name];
    if (!expected) {
        throw new Error('Unexpected test: "' + name + '"');
    }

    if (result !== expected) {
        throw new Error('For test "' + name + '": expected ' + expected + ' actual ' + result);
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

Tester.prototype.end = function() {
    console.log('Passage rate ' + this._passed + ' / ' + this._total);
    process.exitCode = this._total - this._passed;
    process.exit();
};

module.exports = Tester;