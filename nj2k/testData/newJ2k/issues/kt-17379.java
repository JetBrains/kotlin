//file
package demo;

class Test {
  void test() {
    int a = 0;
    int b = 1;
    int c = 2;
    int d = 4;
    int y = a // polyadic expression case
            + b // x2
            + c // x3
            + d; // x4
    int z = a // binary expression case
            + b; // x4
    int j = b +
               c;
  }
}