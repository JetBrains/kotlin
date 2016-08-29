function A(v) {
    this.v = v;
    this.m = function (i, s) {
        return "A.m " + v + " " + i + " " + s;
    }
}

A.prototype.nativeExt = function (i, s) {
    return "nativeExt " + this.v + " " + i + " " + s;
};

A.prototype.nativeExt2AnotherName = function (i, s) {
    return "nativeExt2 " + this.v + " " + i + " " + s;
};
