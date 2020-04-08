package testing;

import testing.rename.*;

class JavaClient {
    public void foo(A a) {
        a.second();
        new B().second();
        new C().second();
        new D().second();
    }

    public static class D implements A {
        @Override
        public int second() {
            return 3;
        }
    }

    public static class E extends D {
        @Override
        public int second() {
            return 4;
        }
    }
}
