// EXPECTED_REACHABLE_NODES: 493
package foo

public class Foo {

    public fun blah(): Int {
        return 5
    }

}

public fun Foo.fooImp(): String {
    return "impl" + blah()
}

public fun Foo.fooExp(): String {
    return "expl" + this.blah()
}

fun box(): String {
    var a = Foo()
    if (a.fooImp() != "impl5") return "fail1: ${a.fooImp()}"
    if (a.fooExp() != "expl5") return "fail2: ${a.fooExp()}"
    return "OK";
}
