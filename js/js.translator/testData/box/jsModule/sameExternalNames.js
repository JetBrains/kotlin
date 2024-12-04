$kotlin_test_internal$.beginModule();

function A(x) {
    this.x = x;
}

module.exports = A;

$kotlin_test_internal$.endModule("foo1");

$kotlin_test_internal$.beginModule();

module.exports = function () {
    return 38
};

$kotlin_test_internal$.endModule("foo2");

$kotlin_test_internal$.beginModule();

module.exports = 39

$kotlin_test_internal$.endModule("foo3");

$kotlin_test_internal$.beginModule();

function B(x) {
    this.x = x;
}

module.exports = B

$kotlin_test_internal$.endModule("bar1");

$kotlin_test_internal$.beginModule();

module.exports = function () {
    return 83
};

$kotlin_test_internal$.endModule("bar2");

$kotlin_test_internal$.beginModule();

module.exports = 93

$kotlin_test_internal$.endModule("bar3");