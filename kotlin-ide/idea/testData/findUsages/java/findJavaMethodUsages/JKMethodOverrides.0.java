// PSI_ELEMENT: com.intellij.psi.PsiMethod
// OPTIONS: overrides
public class A {
    public void <caret>foo() {

    }
}

public class B extends A {
    @Override
    public void foo() {

    }
}