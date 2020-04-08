package test;

public class C extends JavaClass {
    public void foo(JavaClass javaClass) {
        javaClass.field++;
        --javaClass.field;
        myProperty = javaClass.field;
        javaClass.field -= field;
        field = myProperty;
        field *= 2;
        field = field * 2;

        String s = JavaClass.NAME;
    }
}