function A(value) {
  this.value = value;
}

A.prototype.bar = function() { return "A.bar " + this.value; };
