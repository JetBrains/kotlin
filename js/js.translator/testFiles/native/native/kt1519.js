function Wow() {
    this.x = 1;
    this.y = 2;
}

Wow.prototype.sum = function() {
    return this.x + this.y;
};