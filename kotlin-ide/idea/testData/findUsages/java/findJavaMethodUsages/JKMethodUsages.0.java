// PSI_ELEMENT: com.intellij.psi.PsiMethod
// OPTIONS: usages
// FIR_COMPARISON
class A {
    public void <caret>foo() {

    }
}

class B extends A {
    @Override
    public void foo() {

    }
}