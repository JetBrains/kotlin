class C1 {
  C1(int arg1,
     int arg2,
     int arg3) {
  }

  C1(int x,
     int y) {
      this(x, x + y, 0);
  }
}

class C2 {
  private int arg1;
  private int arg2;

  C2(int arg1,
     int arg2,
     int arg3) {
      this.arg1 = arg1;
      this.arg2 = arg2;
  }
}
