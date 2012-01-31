var A = Kotlin.Class.create();
var B = Kotlin.Class.create(A);
var C = Kotlin.Class.create();
var c = new C;

test = function() {
    return ((!isType(c, A)) && !isType(c, B));
}