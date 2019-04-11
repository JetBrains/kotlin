public class A {
    void foo(Object o) {
        if (o == null) return;
        int length = ((String) o).length();
    }
}