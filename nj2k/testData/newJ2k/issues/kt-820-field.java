//file
package demo;

class Container {
  int myInt = 1;
}

class One {
  static Container myContainer = new Container();
}

class Test {
  byte b = One.myContainer.myInt;
}