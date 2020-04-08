// PSI_ELEMENT: com.intellij.psi.PsiMethod
// FIND_BY_REF
// OPTIONS: usages
public class JJ extends B {
    public JJ(int i) {
        super("");
    }

    void test() {
        new <caret>B("");
    }
}