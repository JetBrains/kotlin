var a = {
    o: function () {
        return "O";
    }
};

var b = {
    D: function () {
        this.k = function () {
            return "K";
        };
    }
};

function C() {}

C.prototype.o = function () {
    return a.o();
};

C.D = function () {
    return new b.D();
};