class A {
    private int bar(String s) {
        System.out.println("s = " + s);
        return 0;
    }

    private int bar() {
        return bar(null);
    }
}