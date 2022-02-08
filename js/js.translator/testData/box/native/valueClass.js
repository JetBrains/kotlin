function NativeFoo(s) {
    this.s = s;
}

NativeFoo.prototype.toString = function () {
    return 'NativeFoo(\'' + this.s + '\')'
};

function _describeValue(x) {
    return String(x) + ' (' + typeof x + ')'
}

function describeValueOfProperty(o, name) {
    o = o == null ? globalThis : o;
    return _describeValue(o[name]);
}

function nullifyTestProperties(o) {
    o = o == null ? globalThis : o;

    o.intWrapper = null;
    o.intWrapperN = null;
    o.intNWrapper = null;
    o.intNWrapperN = null;
    o.fooWrapper = null;
    o.fooWrapperN = null;
    o.fooNWrapper = null;
    o.fooNWrapperN = null;
    o.nativeFooWrapper = null;
    o.nativeFooWrapperN = null;
    o.nativeFooNWrapper = null;
    o.nativeFooNWrapperN = null;
}

function defineTestMethodsAndPropertiesOnObject(o) {
    o.describeIntWrapper = _describeValue;
    o.describeIntWrapperN = _describeValue;
    o.describeIntNWrapper = _describeValue;
    o.describeIntNWrapperN = _describeValue;
    o.describeFooWrapper = _describeValue;
    o.describeFooWrapperN = _describeValue;
    o.describeFooNWrapper = _describeValue;
    o.describeFooNWrapperN = _describeValue;
    o.describeNativeFooWrapper = _describeValue;
    o.describeNativeFooWrapperN = _describeValue;
    o.describeNativeFooNWrapper = _describeValue;
    o.describeNativeFooNWrapperN = _describeValue;

    nullifyTestProperties(o);

    o.getIntWrapper = function () { return this.intWrapper; };
    o.getIntWrapperN = function () { return this.intWrapperN; };
    o.getIntNWrapper = function () { return this.intNWrapper; };
    o.getIntNWrapperN = function () { return this.intNWrapperN; };
    o.getFooWrapper = function () { return this.fooWrapper; };
    o.getFooWrapperN = function () { return this.fooWrapperN; };
    o.getFooNWrapper = function () { return this.fooNWrapper; };
    o.getFooNWrapperN = function () { return this.fooNWrapperN; };
    o.getNativeFooWrapper = function () { return this.nativeFooWrapper; };
    o.getNativeFooWrapperN = function () { return this.nativeFooWrapperN; };
    o.getNativeFooNWrapper = function () { return this.nativeFooNWrapper; };
    o.getNativeFooNWrapperN = function () { return this.nativeFooNWrapperN; };

    Object.defineProperties(o, {
        readOnlyIntWrapper: {
            get: o.getIntWrapper
        },
        readOnlyIntWrapperN: {
            get: o.getIntWrapperN
        },
        readOnlyIntNWrapper: {
            get: o.getIntNWrapper
        },
        readOnlyIntNWrapperN: {
            get: o.getIntNWrapperN
        },
        readOnlyFooWrapper: {
            get: o.getFooWrapper
        },
        readOnlyFooWrapperN: {
            get: o.getFooWrapperN
        },
        readOnlyFooNWrapper: {
            get: o.getFooNWrapper
        },
        readOnlyFooNWrapperN: {
            get: o.getFooNWrapperN
        },
        readOnlyNativeFooWrapper: {
            get: o.getNativeFooWrapper
        },
        readOnlyNativeFooWrapperN: {
            get: o.getNativeFooWrapperN
        },
        readOnlyNativeFooNWrapper: {
            get: o.getNativeFooNWrapper
        },
        readOnlyNativeFooNWrapperN: {
            get: o.getNativeFooNWrapperN
        },
    });
}

defineTestMethodsAndPropertiesOnObject(globalThis);

function TestClass(
    intWrapper,
    intWrapperN,
    intNWrapper,
    intNWrapperN,
    fooWrapper,
    fooWrapperN,
    fooNWrapper,
    fooNWrapperN,
    nativeFooWrapper,
    nativeFooWrapperN,
    nativeFooNWrapper,
    nativeFooNWrapperN
) {
    defineTestMethodsAndPropertiesOnObject(this);

    this.intWrapper = intWrapper;
    this.intWrapperN = intWrapperN;
    this.intNWrapper = intNWrapper;
    this.intNWrapperN = intNWrapperN;
    this.fooWrapper = fooWrapper;
    this.fooWrapperN = fooWrapperN;
    this.fooNWrapper = fooNWrapper;
    this.fooNWrapperN = fooNWrapperN;
    this.nativeFooWrapper = nativeFooWrapper;
    this.nativeFooWrapperN = nativeFooWrapperN;
    this.nativeFooNWrapper = nativeFooNWrapper;
    this.nativeFooNWrapperN = nativeFooNWrapperN;
}

defineTestMethodsAndPropertiesOnObject(TestClass);

var TestObject = {};

defineTestMethodsAndPropertiesOnObject(TestObject);

function makeEmptyTestClassInstance() {
    var o = new TestClass();
    nullifyTestProperties(o);
    return o;
}

function makeTestInterfaceInstance() {
    return makeEmptyTestClassInstance();
}
