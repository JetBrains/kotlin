class A {
    void foo(int i, char c, String s) {
        System.out.println("foo" + i + c + s);
    }

    void foo(int i, char c) {
        foo(i, c, "");
    }

    void foo(int i) {
        foo(i, 'a', "");
    }

    int bar(String s) {
        System.out.println("s = " + s);
        return 0;
    }

    int bar() {
        return bar(null);
    }
}