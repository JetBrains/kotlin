// !FORCE_NOT_NULL_TYPES: false
// !SPECIFY_LOCAL_VARIABLE_TYPE_BY_DEFAULT: true
import java.util.HashSet;

class Foo {
  void foo(HashSet o) {
    HashSet o2 = o;
    int foo = 0;
    foo = o2.size();
  }
}