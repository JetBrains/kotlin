class A {
    void foo() {
        for (int i = 1; i < 1000; i *= 2) {
            System.out.println(i);
        }

        for (int i = 1; i < 2000; i *= 2) {
            System.out.println(i);
        }
    }
}