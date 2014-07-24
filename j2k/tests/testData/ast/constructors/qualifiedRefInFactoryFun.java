//file
class C {
    private final int arg1;
    private final int arg2;
    private final int arg3;

    C(int arg1, int arg2, int arg3) {
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.arg3 = arg3;
    }

    C(int arg1, int arg2, C other) {
        this(arg1, arg2, 0);
        System.out.println(this.arg1 + other.arg2);
    }
}

class User {
    void foo() {
        C c1 = new C(100, 100, 100);
        C c2 = new C(100, 100, c1);
    }
}