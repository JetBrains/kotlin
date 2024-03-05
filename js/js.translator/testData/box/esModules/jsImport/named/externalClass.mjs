export function Foo(x) {
    this.x = x;
}

Foo.prototype.foo = function (y) {
    return this.x + y;
};

Foo.prototype.bar = function() {
    return "(" + Array.prototype.join.call(arguments, "") + ")";
};
