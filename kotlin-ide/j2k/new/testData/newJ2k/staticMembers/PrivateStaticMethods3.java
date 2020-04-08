//file
class A {
    public static class Nested {
        void foo() {
            privateStatic1();
        }
    }

    void bar() {
        privateStatic2();
    }

    private static void privateStatic1(){}
    private static void privateStatic2(){}
}