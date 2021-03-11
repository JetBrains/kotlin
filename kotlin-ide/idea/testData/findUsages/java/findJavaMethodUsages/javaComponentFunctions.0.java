// PSI_ELEMENT: com.intellij.psi.PsiMethod
// OPTIONS: usages

public class A {
}

public class JavaClass {
    public A <caret>component1() {
        return new A();
    }

    public int component2() {
        return 0;
    }
}