// PSI_ELEMENT: com.intellij.psi.PsiMethod
// FIND_BY_REF
// OPTIONS: usages
public class JJ extends B {
    public JJ(int i) {
        <caret>super("");
    }

    void test() {
        new B("");
    }
}