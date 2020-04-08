public class ImplementJavaInnerInterface {
    interface Test {
        void <caret>foo();
    }

    void test() {
        Test test = new Test() {
            @Override
            public void foo() {

            }
        };
    }

    public static class OtherJava implements Test {
        @Override
        public void foo() {

        }
    }

    void usage(Test test) {
        test.foo();
    }
}

// REF: (in ImplementJavaInnerInterface.OtherJava).foo()
// REF: (in KotlinTest).foo()
// REF: <anonymous>.foo()