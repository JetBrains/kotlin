class C {
  C(int arg1, int arg2, int arg3) {
  }

  C(int arg1, int arg2) {
    this(arg1, arg2, 0);
    System.out.println();
  }

  C(int arg1) {
    this(arg1, 0);
    System.out.println();
  }
}

public class User {
  public static void main() {
     C c1 = new C(1, 2, 3);
     C c2 = new C(5, 6);
     C c3 = new C(7);
  }
}