//file
class Test {
  void test() {
    boolean res = true;
    res &= false;
    res |= false;
    res ^= false;
    System.out.println(true & false);
    System.out.println(true | false);
    System.out.println(true ^ false);
    System.out.println(!true);

    System.out.println(true && false);
    System.out.println(true || false);
  }
}