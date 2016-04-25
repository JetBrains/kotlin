package foo;

import bar.B;
import bar.UseBKt;

public class JavaClass {
    public void test() {
        A a = new A();

        a.funA();
        a.getValA();

        B b = new B();
        b.useAfromB(a);
        b.funB();
        b.getValB();
        UseBKt.useB(b);

        System.out.println(AGenerated.class.getName());
    }
}