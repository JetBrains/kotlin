function createA() {
    function ADerived() {
    }
    ADerived.prototype = Object.create(JS_TESTS.foo.A.prototype);
    ADerived.prototype.foo_za3lpa$ = function(n) {
        return 24;
    };
    return new ADerived();
}

function createB() {
    function BDerived() {
    }
    BDerived.prototype = Object.create(JS_TESTS.foo.B.prototype);
    BDerived.prototype.bar_za3lpa$ = function(n) {
        return this.foo_za3lpa$(n);
    };
    return new BDerived();
}