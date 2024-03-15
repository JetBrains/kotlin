abstract class Super

class Foo : Super()

fun box(): String {
    if (Foo() is Super) {
        return "OK"
    } else {
        return "FAIL"
    }
}
