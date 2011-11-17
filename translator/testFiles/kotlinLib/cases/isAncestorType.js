var A = Class.create();
var B = Class.create(A);
var b = new B;

test = function() {
    return (isType(b, A) && isType(b, B));
}