public class Test {
  private final String myName;
  boolean a;
  double b;
  float c;
  long d;
  int e;
  protected short f;
  protected char g;

  public Test() {}

  public Test(String name) {
    myName = foo(name);
  }

  static String foo(String n) {return "";}
}

public class User {
  public static void main() {
     Test t = new Test("name");
  }
}