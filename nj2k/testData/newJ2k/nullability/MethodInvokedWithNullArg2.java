//file
class C {
    public void foo(String s){}
}

class D {
    void bar(C c) {
        c.foo(null);
    }
}