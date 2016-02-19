package test2;

import test.*;

public class DerivedOtherPackage extends BaseOtherPackage {
    public void usage1() {
        BaseOtherPackage base = new BaseOtherPackage();
        base.foo();
        int i = base.i;
    }
}