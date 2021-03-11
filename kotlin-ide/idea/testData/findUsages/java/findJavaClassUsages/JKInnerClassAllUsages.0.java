// PSI_ELEMENT: com.intellij.psi.PsiClass
// OPTIONS: usages
public class Outer {
    public class <caret>A {
        public String bar = "bar";

        public A() {

        }

        public void foo() {

        }
    }
}