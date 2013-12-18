//file
package demo;

class Test {
  void test() {
    String name = "$$$$";
    name = name.replaceAll("\\$[0-9]+", "\\$")

    char c = '$';
    System.out.println(c);

    Character C = '$';
    System.out.println(C);
  }
}