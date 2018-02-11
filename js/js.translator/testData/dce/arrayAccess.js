(function(_) {
    var x = {};
    function foo() {
        return x["bar"]();
    }
    x.bar = function() {
        return "bar";
    };
    x.baz = function() {
        return "baz";
    };
    _.foo = foo;
})(module.exports);

// REQUEST_REACHABLE: main.foo
// ASSERT_REACHABLE: x.bar
// ASSERT_UNREACHABLE: x.baz