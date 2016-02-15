function B(value) {
    this.foo = 100 + value;
}
B.prototype = {};
B.prototype.bar = function() {
    return this.foo + 1000;
};