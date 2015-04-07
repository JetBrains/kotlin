package test;

class C extends JavaClass {
    public void foo(JavaClass javaClass) {
        javaClass.field++;
        --javaClass.field;
        myProperty = javaClass.field;
        javaClass.field -= field;
        field = myProperty;
        field *= 2;
    }
}