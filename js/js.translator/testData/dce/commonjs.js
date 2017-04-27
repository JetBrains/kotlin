function foo() {
}
function bar() {
}
function ignore() {
}

bar();

module.exports.foo = foo;
module.exports.bar = bar;
module.exports.ignore = ignore;

// REQUEST_REACHABLE: main.foo
// ASSERT_REACHABLE: main.bar
// ASSERT_REACHABLE: main.foo
// ASSERT_UNREACHABLE: main.ignore