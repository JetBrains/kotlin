class A {
    void foo() {
        for (int i = 1, j = 0; i < 1000; i *= 2, j++) {
            System.out.println(i);
        }

        for (int j = 1; j < 2000; j *= 2) {
            System.out.println(j);
        }
    }
}