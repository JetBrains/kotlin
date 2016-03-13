package test;

public class BaseProtectedConstructor {
    protected void foo() {

    }
}

class MiddleSamePackage extends BaseProtectedConstructor {
}

class DerivedSamePackage extends MiddleSamePackage {
    void usage() {
        foo();
    }
}
