class A {
    void foo(boolean p) {
        if (p)
            for (int i = 1; i < 1000; i *= 2) {
                System.out.println(i);
            }
    }
}