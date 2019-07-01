//file
package demo;

class Test {
  void putInt(Integer i) {}

  void test() {
    byte b = 10;
    putInt(b);

    Byte b2 = 10;
    putInt(b2);
  }
}