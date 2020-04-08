// "Add 'var' property 'foo' to 'TestKt'" "true"
// WITH_RUNTIME
class J {
    void test() {
        String s = TestKt.<caret>foo;
    }
}