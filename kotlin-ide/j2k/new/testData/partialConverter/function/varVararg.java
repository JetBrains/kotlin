//file
package demo;

class Test {
  void <caret>test(Object ... args) {
    args = new Integer[] {1, 2, 3};
  }
}
