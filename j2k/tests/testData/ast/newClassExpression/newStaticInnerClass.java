//file
package demo;

class Foo {
  static class Bar {}
}

class User {
  void main() {
    Foo.Bar boo = new Foo.Bar();
  }
}