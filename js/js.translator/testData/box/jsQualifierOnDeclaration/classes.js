var pkg = function() {
    function C() {
    }
    C.prototype.o = function() {
        return "O";
    };
    function D() {
    }
    D.prototype.k = function() {
        return "K";
    };
    C.D = D;
    return {
        C: C
    };
}();