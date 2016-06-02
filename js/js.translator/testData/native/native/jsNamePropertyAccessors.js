function A() {
    this.y_ = 0;
}
A.prototype.getX = function() {
    return 23;
};
A.prototype.getY = function() {
    return this.y_;
};
A.prototype.setY = function(y) {
    return this.y_ = y;
};

var z_ = 32;
function getZ() {
    return z_;
}
function setZ(z) {
    z_ = z;
}