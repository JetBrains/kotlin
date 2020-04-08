//method
// !SPECIFY_LOCAL_VARIABLE_TYPE_BY_DEFAULT: true
String bar() {
    return null;
}
void foo() {
  String s = bar();
  if (s != null) {
      zoo(s);
  }
}