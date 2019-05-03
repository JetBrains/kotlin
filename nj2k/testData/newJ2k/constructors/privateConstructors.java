class C {
  private C(int arg1, int arg2, int arg3) {
  }

  private C(int arg1, int arg2) {
    this(arg1, arg2, 0);
  }

  public C(int arg1) {
    this(arg1, 0, 0);
  }
}
