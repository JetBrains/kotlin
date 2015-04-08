interface I {
    void foo(int i, char c, String s);
    void bar();
}

class A implements I{
    public void foo(int i, char c, String s) {
        System.out.println("foo" + i + c + s);
    }

    public void foo(int i, char c) {
        foo(i, c, "");
    }

    public void foo(int i) {
        foo(i, 'a', "");
    }

    public void bar() {
        bar(1);
    }

    public void bar(int i) {}

    public void x() {
        x(1);
    }

    public void x(int i) {}

    public void y() {
        y(1);
    }

    public void y(int i) {}
}

class B extends A {
    public void x() {
        super.x();
    }

    public void y(int i) {
        super.y(i);
    }
}