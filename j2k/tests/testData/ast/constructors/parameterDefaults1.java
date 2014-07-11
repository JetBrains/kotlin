package pack

class C {
  C(int a, int b, int c, int d, int e) {
  }

  C(int a, int b, int c) {
    this(a, b, c, 0, 0);
  }

  C(int a) {
    this(a, 0, 0, 0, 1);
  }

  C() {
    this(0, 0, 0, 0, 0);
  }
}
