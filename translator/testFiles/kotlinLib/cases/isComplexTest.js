var A = Kotlin.Class.create();
var B = Kotlin.Class.create(A);
var b = new B;
var C = Kotlin.Class.create(B);
var c = new C;
var E = Kotlin.Class.create(A)
var e = new E;

test1 = function() {
    b2 = b
    return (Kotlin.isType(b, A) && Kotlin.isType(b, B));
}

test2 = function() {
    return (Kotlin.isType(c, C) && Kotlin.isType(c, B) && Kotlin.isType(c, A) && (!Kotlin.isType(c, E)));
}

test3 = function() {
    return Kotlin.isType(e, E) && !Kotlin.isType(e, B) && !Kotlin.isType(e, C) && Kotlin.isType(e, A)
}

test = function() {
    return test1() && test2() && test3()
}