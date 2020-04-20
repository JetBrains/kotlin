// PSI_ELEMENT: com.intellij.psi.PsiMethod
// OPTIONS: overrides
public interface Foo2 {
    interface X<T> {}
    void <caret>baz(X  clazz);
}