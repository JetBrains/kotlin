// !forceNotNullTypes: false
// !specifyLocalVariableTypeByDefault: true
import java.util.BitSet;

class Foo {
  void foo(BitSet o) {
    BitSet o2 = o;
    int foo = 0;
    foo = o2.size();
  }
}