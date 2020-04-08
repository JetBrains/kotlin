import org.jetbrains.annotations.NotNull;

import java.lang.Override;

class J extends A {
    private int p;

    public J() {
        super(0);
    }

    @NotNull
    @Override
    public String getS() {
        return p;
    }

    @Override
    public void setS(@NotNull String value) {
        p = value;
    }
}

class Test {
    static void test() {
        new A(0).getS();
        new A(0).setS(1);

        new B(0).getS();
        new B(0).setS(2);

        new C().getS();
        new C().setS(3);

        new J().getS();
        new J().setS(4);

        new D().getS();
        new D().setS(5);

        new E().getS();
        new E().setS(6);
    }
}