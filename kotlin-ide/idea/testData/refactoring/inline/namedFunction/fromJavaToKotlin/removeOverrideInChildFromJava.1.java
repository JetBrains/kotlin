public class A {
    public void <caret>a() {

    }
}

class JavaClass2 extends A {
    @Override
    public void a() {

    }
}

class M extends A {
    @Override
    public void a() {

    }
}

class M2 extends A {
    @Override
    public void a() {

    }
}

interface JavaInterface {
    void a();
}

class JavaClass3 extends A implements JavaInterface {
    @Override
    public void a() {

    }
}

interface NextJavaInterface extends JavaInterface {
}

class JavaClass4 extends A implements NextJavaInterface {
    @Override
    public void a() {

    }
}

class JavaClass5 extends JavaClass4 {
    @Override
    public void a() {

    }
}
