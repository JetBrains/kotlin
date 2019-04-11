//method
// !specifyLocalVariableTypeByDefault: true
void foo(boolean b) {
  String s = "abc";
  if (b) {
      s = null;
  }
}