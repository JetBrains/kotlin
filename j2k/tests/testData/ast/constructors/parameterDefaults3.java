package pack

class C {
  C(int a, int b, int c, int d, int e) {
  }

  C(int a, int b, int c) {
    this(b, a, c, 0, 0);
  }

  C() {
    this(0, 0, 0, 0, 0);
  }
}
