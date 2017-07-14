var tape = require('tape');
var kotlin_test = require('kotlin-test');

var suiteContext = {
    test: tape
};
var hasTests = false;

kotlin_test.setAdapter({
    suite: function (name, ignored, fn) {
        suiteContext.test(name, { skip: ignored }, function(t) {
            var prevContext = suiteContext;
            suiteContext = t;
            hasTests = false;
            fn();
            suiteContext = prevContext;
            if (!hasTests) {
                t.pass('fake suite assert');
            }
            t.end();
        });
    },

    test: function (name, ignored, fn) {
        hasTests = true;
        suiteContext.test(name, { skip: ignored }, function (t) {
            try {
                fn();
            } catch (e) {
                t.ok(false, e.message);
            }
            t.pass('fake assert');
            t.end();
        });
    }
});
