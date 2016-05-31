var bar = function() { return 23; };
var baz = 123;

function B() {
    this.q = 32;
}
B.prototype.g = function() { return 42; };
B.q = 132;
B.g = function() { return 142; };

var P = {
    f: function() { return 222; }
};