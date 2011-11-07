class B {
  void call() {}
}

class A extends B {
  A() {
    super();
  }

  void call() { return super.call(); }
}