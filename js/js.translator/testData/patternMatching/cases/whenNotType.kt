package foo

class A() {

}

fun box(): Boolean {
    when(A()) {
        !is A -> return false;
        else -> return true;
    }
}