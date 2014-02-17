//file
package com.voltvoodoo.saplo4j.model;

import java.io.Serializable;

public class Language implements Serializable {
        protected String code;

        public Language(String code) {
                this.code = code;
        }

        public String toString() {
                return this.code;
        }
}


class Base {
  void test() {}
  String toString() {
    return "BASE";
  }
}

class Child extends Base {
  void test() {}
  String toString() {
      return "Child";
  }
}
