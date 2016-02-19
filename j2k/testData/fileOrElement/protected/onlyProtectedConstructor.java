package test;

public class BaseProtectedConstructor {
    protected BaseSamePackage() {

    }
}

class DerivedSamePackage extends BaseProtectedConstructor {
    DerivedSamePackage() {
        super();
    }
}
