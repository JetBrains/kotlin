(function(_, Kotlin) {
    Kotlin.defineInlineFunction("bar", Kotlin.wrapFunction(function() {
        function baz() {
            return "bar";
        }
        return function() {
            return baz();
        };
    }));
    return _;
})(module.exports, require("kotlin"));

(function(_) {
    function baz() {
        return "baz";
    }

    function foo() {
       return baz();
    }
    _.foo = foo;
})(module.exports);


// REQUEST_REACHABLE: main.foo
// ASSERT_REACHABLE: main.foo
// ASSERT_REACHABLE: baz