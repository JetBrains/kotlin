//file
package demo;

class TestT {
  <T> void getT() { }
}

class U {
  void main() {
    TestT t = new TestT();
    t.<String>getT();
    t.<Integer>getT();
    t.getT();
  }
}