package test2;

import test.A;

class Test {
    A.B.C foo() {
        return new A().new B().new C();
    }
}