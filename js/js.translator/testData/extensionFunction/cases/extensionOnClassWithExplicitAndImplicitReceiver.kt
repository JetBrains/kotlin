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

fun box(): Boolean {
    var a = Foo()
    if (a.fooImp() != "impl5") return false
    if (a.fooExp() != "expl5") return false
    return true;
}
