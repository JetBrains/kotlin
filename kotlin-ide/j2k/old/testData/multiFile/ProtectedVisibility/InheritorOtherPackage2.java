package test2;

import test.*;

public class DerivedOtherPackage extends BaseOtherPackage {
    protected DerivedOtherPackage() {
        super();
        foo();
        int i = this.i;
    }
}