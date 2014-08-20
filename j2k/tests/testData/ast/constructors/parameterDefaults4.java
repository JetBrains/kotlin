package pack

class C {
  C(int a, int b, int c, int d, int e) {
  }

  C(int a, int b, int c) {
    this(a, b, c, 4, 5);
  }

  C(int a) {
    this(a, 2, 3);
  }

  C(int a, int b) {
    this(a, b, 3, 4, 5);
  }

  C() {
    this(1);
  }
}
