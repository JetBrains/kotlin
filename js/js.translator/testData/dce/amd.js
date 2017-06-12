define(['exports'], function (exports) {
    function foo() {
    }
    function bar() {
    }
    function ignore() {
    }

    bar();

    exports.foo = foo;
    exports.bar = bar;
    exports.ignore = ignore;
});

// REQUEST_REACHABLE: main.foo
// ASSERT_REACHABLE: main.foo
// ASSERT_REACHABLE: main.bar
// ASSERT_UNREACHABLE: main.ignore