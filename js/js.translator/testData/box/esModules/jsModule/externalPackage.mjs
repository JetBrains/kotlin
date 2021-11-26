export function A(x) {
    this.x = x;
}
A.prototype.foo = function (y) {
    return this.x + y;
};

function Nested() {
    this.y = 55;
}
A.Nested = Nested;

export var B = {
    x: 123,
    foo: function(y) {
        return this.x + y;
    }
};

export function foo(y) {
    return 323 + y;
}

export var bar = 423;
