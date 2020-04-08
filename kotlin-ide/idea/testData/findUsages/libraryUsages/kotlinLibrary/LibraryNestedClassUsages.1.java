package usages

import library.*

class J extends A.T {
    public J(int n) {
        super(n);
    }

    static void test() {
        A.T t = new A.T();
        A.T tt = new A.T(1);
    }
}