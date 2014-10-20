//class
class B {
  B(int i) {}
  int call() {return 1;}
}

class A extends B {
  A() {
    super(10);
  }

  int call() { return super.call(); }
}