class C {
  final int myArg1;
  int myArg2;
  int myArg3;

  C(int arg1, int arg2, int arg3) {
    this(arg1);
    myArg2 = arg2;
    myArg3 = arg3;
  }

  C(int arg1, int arg2) {
    this(arg1);
    myArg2 = arg2;
    myArg3 = 0;
  }

  C(int arg1) {
    myArg1 = arg1;
    myArg2 = 0;
    myArg3 = 0;
  }
}

public class User {
  public static void main() {
     C c1 = new C(100, 100, 100);
     C c2 = new C(100, 100);
     C c3 = new C(100);
  }
}