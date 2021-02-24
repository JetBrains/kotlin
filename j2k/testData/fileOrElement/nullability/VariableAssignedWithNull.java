//method
// !SPECIFY_LOCAL_VARIABLE_TYPE_BY_DEFAULT: true
void foo(boolean b) {
  String s = "abc";
  if (b) {
      s = null;
  }
}