//file
abstract class A {}

class C {
    void foo() {
        A a = new A() {
            @Override
            public String toString() {
                return "a";
            }
        };
    }
}
