// "Add 'lateinit var' property 'foo' to 'K'" "true"
// WITH_RUNTIME
class J {
    void test(K k) {
        String s = k.<caret>foo;
    }
}