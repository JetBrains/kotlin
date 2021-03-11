// PSI_ELEMENT: com.intellij.psi.PsiMethod
// OPTIONS: usages
// PLAIN_WHEN_NEEDED

public class JavaWithGroovyInvoke_0 {
    public void <caret>invoke() {
    }

    public static class OtherJavaClass extends JavaWithGroovyInvoke_0 {}
}