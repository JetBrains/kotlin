class A {
    int i = 1;

    void foo() {
        for (int i = 1; i < 1000; i *= 2) {
            System.out.println(i);
        }

        i = 10;
    }
}