package demo;

import java.util.HashMap;

class Test {
  Test() {  }
  Test(String s) {  }
}

class User {
  void main() {
    HashMap m = new HashMap(1);
    HashMap m2 = new HashMap(10);

    Test t1 = new Test();
    Test t2 = new Test("");
  }
}