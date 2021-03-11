//file
package demo;

class Container {
  int myInt = 1;
}

class One {
  static Container myContainer = new Container();
}

class IntContainer {
  IntContainer(int i) {}
}

class Test {
  void putInt(int i) { }
  void test() {
    putInt(One.myContainer.myInt);
    new IntContainer(One.myContainer.myInt);
  }
}