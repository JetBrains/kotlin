package test2;

import test.A;
import test.A.B;

class Test {
    B.C foo() {
        return new A().new B().new C();
    }
}