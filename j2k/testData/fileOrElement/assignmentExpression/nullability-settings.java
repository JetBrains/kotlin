// !forceNotNullTypes: false
// !specifyLocalVariableTypeByDefault: true

class Foo {
  void foo(HashSet o) {
    HashSet o2 = o;
    int foo = 0;
    foo = o2.size();
  }
}