package test;

public class BaseProtectedConstructor {
    protected void usageInConstructor() {

    }

    protected int usageInPropertyInitializer() {
        return 1;
    }

    protected void usageInStaticInit() {

    }

    protected void usageInMethod() {

    }
}

class DerivedSamePackage {
    DerivedSamePackage() {
        new BaseProtectedConstructor().usageInConstructor();
    }

    private int i = new BaseProtectedConstructor().usageInPropertyInitializer();

    static {
        new BaseProtectedConstructor().usageInStaticInit();
    }

    void usage() {
        new BaseProtectedConstructor().usageInMethod();
    }
}
