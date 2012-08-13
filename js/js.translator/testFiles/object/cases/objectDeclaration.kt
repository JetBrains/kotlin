package foo

class A {

}

object test {
    var c = 2;
    var b = 1;
}

object aWrapper {
    var a = A();
}

fun box(): Boolean {
    if (test.c != 2) return false;

    if (test.b != 1) return false;
    test.c += 10
    if (test.c != 12) {
        return false;
    }
    if (aWrapper.a !is A) {
        return false
    }
    return true;
}