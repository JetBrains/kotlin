//method
int foo(String s, boolean b) {
  if (s == null) System.out.println("null")
  if (b) return s.length();
  return 10;
}