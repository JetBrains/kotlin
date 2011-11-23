var A = Class.create();
var B = Class.create(A);
var C = Class.create();
var c = new C;

test = function() {
    return ((!isType(c, A)) && !isType(c, B));
}