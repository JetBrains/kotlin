var A = Class.create();
var B = Class.create(A);
var b = new B;
var C = Class.create(B);
var c = new C;
var E = Class.create(A)
var e = new E;

test1 = function() {
    b2 = b
    return (isType(b, A) && isType(b, B));
}

test2 = function() {
    return (isType(c, C) && isType(c, B) && isType(c, A) && (!isType(c, E)));
}

test3 = function() {
    return isType(e, E) && !isType(e, B) && !isType(e, C) && isType(e, A)
}

test = function() {
    return test1() && test2() && test3()
}