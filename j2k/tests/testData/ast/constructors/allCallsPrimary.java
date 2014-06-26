//file
package pack

class C {
  C(int arg1, int arg2, int arg3) {
  }

  C(int arg1, int arg2) {
    this(arg1, arg2, 0);
  }

  C(int arg1) {
    this(arg1, 0, 0);
  }
}

public class User {
  public static void main() {
     C c1 = new C(100, 100, 100);
     C c2 = new C(100, 100);
     C c3 = new C(100);
  }
}