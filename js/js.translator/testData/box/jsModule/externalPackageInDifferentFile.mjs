function A(x) {
    this.x = x;
}
A.prototype.foo = function (y) {
    return this.x + y;
};

var B = {
    x: 123,
    foo: function(y) {
        return this.x + y;
    }
};

function foo(y) {
    return 323 + y;
}

var bar = 423;

export { A, B, foo, bar };