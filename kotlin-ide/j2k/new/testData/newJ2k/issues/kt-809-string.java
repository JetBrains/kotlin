//file
package demo;

class Container {
  String myString = "1";
}

class One {
  static Container myContainer = new Container();
}

class StringContainer {
  StringContainer(String s) {}
}

class Test {
  void putString(String s) { }
  void test() {
    putString(One.myContainer.myString);
    new StringContainer(One.myContainer.myString);
  }
}