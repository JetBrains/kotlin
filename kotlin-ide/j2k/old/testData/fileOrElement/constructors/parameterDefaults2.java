package pack

class C {
  C(int a, int b, int c, int d, int e) {
  }

  C(int a1, int b1, int c1) {
    this(a1, b1, c1, 0, 0);
  }

  C(byte b) {
    this(b, 0, 0, 0, 0);
  }

  C() {
    this(0, 0, 0, 0, 0);
  }
}
