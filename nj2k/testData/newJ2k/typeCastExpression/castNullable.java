class A {
    private String foo(Object o, boolean b) {
        if (b) return (String) o;
        return "";
    }

    void bar() {
        if (foo(null, true) == null) {
            System.out.println("null");
        }
    }
}