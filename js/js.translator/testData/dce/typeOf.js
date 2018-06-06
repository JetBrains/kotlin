(function(_) {
    var x = {};
    x.y = {};
    x.y.bar = function() {
        return "bar";
    };
    function foo() {
        return typeof x.y;
    }
    _.foo = foo;
})(module.exports);

// REQUEST_REACHABLE: main.foo
// ASSERT_REACHABLE: x.y
// ASSERT_UNREACHABLE: x.y.bar