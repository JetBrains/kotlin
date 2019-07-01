//file
class A {
    public void foo() {
        privateStatic1();
        privateStatic2();
    }

    public static void publicStatic(){
        privateStatic1();
    }

    private static void privateStatic1(){}
    private static void privateStatic2(){}
}