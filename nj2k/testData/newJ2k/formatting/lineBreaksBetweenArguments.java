class F {
  void f1(int p1, int p2, int p3, int p4, int... p5) {
  }

  void f2(int[] array) {
    f1(1, 2,
       3, 4,
       array
    );
  }
}