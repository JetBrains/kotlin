package test2;

import test.A;
import test.B;

class Test {
    B foo() {
        return new A().new B();
    }
}