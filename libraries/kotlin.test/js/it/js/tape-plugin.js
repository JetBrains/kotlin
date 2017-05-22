var tape = require('tape');
var kotlin_test = require('kotlin-test');

var setAssertHook = function(t) {
    kotlin_test.setAssertHook(function (result, expected, actual, lazyMessage) {
        t.ok(result, lazyMessage());
        if (!result) {
            t.end();
        }
    });
};


var suiteContext;

kotlin_test.setAdapter({
    suite: function (name, fn) {
        tape(name, function(t) {
            var prevContext = suiteContext;
            suiteContext = t;
            fn();
            suiteContext = prevContext;
        });
    },

    xsuite: function (name, fn) {
        tape.skip(name, function(t) {
            var prevContext = suiteContext;
            suiteContext = t;
            fn();
            suiteContext = prevContext;
        });
    },

    fsuite: function (name, fn) {
        tape(name, function(t) {
            var prevContext = suiteContext;
            suiteContext = t;
            fn();
            suiteContext = prevContext;
        });
    },

    test: function (name, fn) {
        suiteContext.test(name, function (t) {
            setAssertHook(t);
            fn();
            t.end();
        });
    },

    xtest: function (name, fn) {
        suiteContext.test(name, { skip: true}, function (t) {
            setAssertHook(t);
            fn();
            t.end();
        });
    },

    ftest: function (name, fn) {
        suiteContext.test(name, function (t) {
            setAssertHook(t);
            fn();
            t.end();
        });
    }
});
