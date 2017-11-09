function A() {
}
A.prototype = Object.create(A);
A.prototype.constructor = A;
