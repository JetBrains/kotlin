function actualTypeOfChar(x) {
    return typeof x;
}

function actualTypeOfMaybeChar(x) {
    return actualTypeOfChar(x);
}

function getSomeChar() {
    return 'a'.charCodeAt(0);
}

function getMaybeChar() {
    return getSomeChar();
}

function getCharNull() {
    return null;
}

var charVal = getSomeChar();
var maybeCharVal = getMaybeChar();
var charNullVal = getCharNull();

var charVar = getSomeChar();
var maybeCharVar = null;

function A(c) {
    this.c = c;
    this._baz = 'q'.charCodeAt(0);
    this._nullableBaz = null;
}

A.prototype.foo = function () {
    return this.c;
};

A.prototype.maybeFoo = A.prototype.foo;
A.prototype.fooNull = getCharNull;

A.prototype.actualTypeOfChar = actualTypeOfChar
A.prototype.actualTypeOfMaybeChar = actualTypeOfMaybeChar

Object.defineProperty(A.prototype, 'bar', {
    configurable: true,
    get: A.prototype.foo
});

Object.defineProperty(A.prototype, 'maybeBar', {
    configurable: true,
    get: A.prototype.maybeFoo
});

Object.defineProperty(A.prototype, 'barNull', {
    configurable: true,
    get: A.prototype.fooNull
});

Object.defineProperty(A.prototype, 'baz', {
    configurable: true,
    get: function () { return this._baz; },
    set: function (v) { this._baz = v; }
});

Object.defineProperty(A.prototype, 'maybeBaz', {
    configurable: true,
    get: function () { return this._nullableBaz; },
    set: function (v) { this._nullableBaz = v; }
});
