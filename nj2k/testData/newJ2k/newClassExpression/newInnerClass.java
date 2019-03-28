//file
package org.test;

class OuterClass {
  class InnerClass {
  }
}

class User {
  void main() {
    OuterClass outerObject = new OuterClass();
    OuterClass.InnerClass innerObject = outerObject.new InnerClass();
  }
}