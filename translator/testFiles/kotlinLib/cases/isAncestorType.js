var A = Kotlin.Class.create();
var B = Kotlin.Class.create(A);
var b = new B;

test = function() {
    return (Kotlin.isType(b, A) && Kotlin.isType(b, B));
}