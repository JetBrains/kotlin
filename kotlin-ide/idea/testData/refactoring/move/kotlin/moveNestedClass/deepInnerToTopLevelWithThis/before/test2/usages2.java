package test2;

import test.A;
import test.A.B.C;

class Test {
    C foo() {
        return new A().new B().new C();
    }
}