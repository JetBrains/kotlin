package foo

var c = 0

class A() {
    var  p = 0;
    {
        c++;
    }
}

fun box(): Boolean {
    ++A().p
    if (c != 1) {
        return false;
    }
    --A().p
    if (c != 2) {
        return false;
    }
    return true
}