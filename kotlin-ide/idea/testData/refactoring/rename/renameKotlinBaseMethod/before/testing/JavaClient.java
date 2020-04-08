package testing;

import testing.rename.*;

class JavaClient {
    public void foo(A a) {
        a.first();
        new B().first();
        new C().first();
        new D().first();
    }

    public static class D implements A {
        @Override
        public int first() {
            return 3;
        }
    }

    public static class E extends D {
        @Override
        public int first() {
            return 4;
        }
    }
}
