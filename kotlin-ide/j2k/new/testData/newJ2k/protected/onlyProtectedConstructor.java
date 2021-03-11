package test;

public class BaseProtectedConstructor {
    protected BaseProtectedConstructor() {

    }
}

class DerivedSamePackage extends BaseProtectedConstructor {
    DerivedSamePackage() {
        super();
    }
}
