//method
// !specifyLocalVariableTypeByDefault: true
String bar() {
    return null;
}
void foo() {
  String s = bar();
  if (s != null) {
      zoo(s);
  }
}