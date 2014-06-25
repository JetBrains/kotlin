//file
class C {
    private static int staticField1 = 0;
    private static int staticField2 = 0;

    C() {
    }

    C(int p) {
        this();
        System.out.println(staticField1 + C.staticField2);
    }
}
