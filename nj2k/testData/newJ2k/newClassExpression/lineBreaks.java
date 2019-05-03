class C {
  C(int p1, int p2, int p3){}
}

class User {
  void foo() {
    new C(1,
          2,
          3);
  }
}