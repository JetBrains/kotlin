class Base {
  void foo() {}
}

class A extends Base {
  class C {
    void test() {
      A.this.foo();
    }
  }
}