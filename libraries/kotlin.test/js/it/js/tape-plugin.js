var tape = require('tape');
var kotlin_test = require('kotlin-test');

var setAssertHook = function(t) {
    kotlin_test.setAssertHook(function (result) {
        t.ok(result.result, result.lazyMessage());
        if (!result.result) {
            t.end();
        }
    });
};


var suiteContext = {
    test: tape
};

kotlin_test.setAdapter({
    suite: function (name, fn) {
        suiteContext.test(name, function(t) {
            var prevContext = suiteContext;
            suiteContext = t;
            fn();
            suiteContext = prevContext;
            t.pass('fake suite assert');
            t.end();
        });
    },

    xsuite: function (name, fn) {
        suiteContext.test(name, {skip: true}, function(t) {
            var prevContext = suiteContext;
            suiteContext = t;
            fn();
            suiteContext = prevContext;
            t.pass('fake suite assert');
            t.end();
        });
    },

    fsuite: function (name, fn) {
        suiteContext.test(name, function(t) {
            var prevContext = suiteContext;
            suiteContext = t;
            fn();
            suiteContext = prevContext;
            t.pass('fake suite assert');
            t.end();
        });
    },

    test: function (name, fn) {
        suiteContext.test(name, function (t) {
            setAssertHook(t);
            fn();
            t.pass('fake assert');
            t.end();
        });
    },

    xtest: function (name, fn) {
        suiteContext.test(name, { skip: true}, function (t) {
            setAssertHook(t);
            fn();
            t.pass('fake assert');
            t.end();
        });
    },

    ftest: function (name, fn) {
        suiteContext.test(name, function (t) {
            setAssertHook(t);
            fn();
            t.pass('fake assert');
            t.end();
        });
    }
});
