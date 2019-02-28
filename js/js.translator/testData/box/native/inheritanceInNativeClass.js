function createA() {
    function ADerived() {
    }
    ADerived.prototype = Object.create(JS_TESTS.A.prototype);
    ADerived.prototype.foo = function(n) {
        return 24;
    };
    return new ADerived();
}

function createB() {
    function BDerived() {
    }
    BDerived.prototype = Object.create(JS_TESTS.B.prototype);
    BDerived.prototype.bar = function(n) {
        return this.foo(n);
    };
    return new BDerived();
}