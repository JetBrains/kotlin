//file
package demo;

class Test {
  String test() {
    String s1 = "";
    String s2 = "";
    String s3 = "";
    if (s1.isEmpty() && s2.isEmpty())
      return "OK";

    if (s1.isEmpty() && s2.isEmpty() && s3.isEmpty())
      return "OOOK";

    return "";
  }
}