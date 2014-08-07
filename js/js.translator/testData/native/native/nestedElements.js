var Object = createTestObject("Object", 23);
extend(Object, {
    Object: extend(createTestObject("Object.Object", 123), { AnotherClass : createTestClass("Object.Object.Class", 42, 142) }),
    Class: createTestClass("Object.Class", 42, 142),
    Trait : createTestObject("Object.Trait", 324),
    a: createTestObject("Object.a", 34)
});

var SomeClass = function () {};
extend(SomeClass, createTestObject("Class", 77));
extend(SomeClass, {
    Object: createTestObject("Class.Object", 55),
    Class: createTestClass("Class.Class", 66, 88),
    InnerClass: createTestInnerClass("Class.InnerClass", 57),
    Trait: createTestObject("Class.Trait", 55),
    aaa: createTestObject("Class.a", 22)
});

var Trait = createTestObject("Trait", 277);
extend(Trait, {
    SomeObject: createTestObject("Trait.Object", 90),
    Class: createTestClass("Trait.Class", 66, 88),
    SomeTrait: createTestObject("Trait.Trait", 55),
    a: createTestObject("Trait.a", 22)
});

// Helpers

function extend(destination, source) {
    for (var property in source) {
        if (source.hasOwnProperty(property)) {
            destination[property] = source[property];
        }
    }
    return destination;
}

function createTestClass(fqName, memberFunResult, staticFunResult) {
    function Class(a) {
        this.a = a;
        this.b = fqName + "().b"
    }

    Class.prototype.test = function () { return memberFunResult };

    extend(Class, createTestObject(fqName, staticFunResult));

    return Class;
}

function createTestInnerClass(fqName, memberFunResult) {
    function Class(parent, a) {
        this.a = a;
        this.b = fqName + "().b"
    }

    Class.prototype.test = function () { return memberFunResult };
}

function createTestObject(fqName, funResult) {
    return {
        a: fqName + ".a",
        b: fqName + ".b",
        test: function () { return funResult }
    }
}
