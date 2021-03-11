// "Add method 'foo' to 'Dep'" "true"
// RUNTIME_WITH_JDK_10
class J {
    void test() {
        Dep dep = new Dep();
        var foo = dep.<selection><caret></selection>foo();
    }
}