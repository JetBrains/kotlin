class C {
    private int field;

    C(int arg1, int arg2, int arg3) {
        arg1++;
        System.out.print(arg1 + arg2);
        field = arg3;
        arg3++;
    }

    C(int arg1, int arg2) {
        this(arg1, arg2, 0);
        arg2++;
    }

    C(int arg1) {
        this(arg1, 0, 0);
    }
}

public class User {
    public static void main() {
        C c1 = new C(100, 100, 100);
        C c2 = new C(100, 100);
        C c3 = new C(100);
    }
}