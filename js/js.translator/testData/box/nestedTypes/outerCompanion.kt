// EXPECTED_REACHABLE_NODES: 499
package foo

class A {
    inner class B {
        val x = foo();
    }

    class C {
        val x = foo();
    }

    companion object {
        fun foo(): String {
            return "foo_result";
        }
    }
}

fun box(): String {
    var result = A().B().x
    if (result != "foo_result") {
        return "fail1_" + result
    }
    result = A.C().x
    if (result != "foo_result") {
        return "fail2_" + result
    }
    return "OK"
}