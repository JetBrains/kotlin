package test2;

import test.A;

class Test {
    void foo() {
        int x = new A().getTest();
        new A().setTest(1);
    }
}