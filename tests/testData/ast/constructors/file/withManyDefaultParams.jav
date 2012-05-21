public class Test {
  private final String myName;
  private boolean a;
  private double b;
  private float c;
  private long d;
  private int e;
  private short f;
  private char g;

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