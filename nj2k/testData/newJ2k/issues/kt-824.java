//file
package demo;

class Container {
  boolean myBoolean = true;
}

class One {
  static Container myContainer = new Container();
}

class Test {
  void test() {
    if (One.myContainer.myBoolean)
      System.out.println("Ok");

    String s = One.myContainer.myBoolean ? "YES" : "NO";

    while (One.myContainer.myBoolean)
      System.out.println("Ok");

    do {
      System.out.println("Ok");
    } while (One.myContainer.myBoolean)
  }
}