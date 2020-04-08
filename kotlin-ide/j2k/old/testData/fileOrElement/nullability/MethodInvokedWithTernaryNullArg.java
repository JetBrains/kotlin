//file
class C {
    private void foo(String s){}

    void bar(boolean b) {
        foo(b ? "a" : null);
    }
}