function test(x) {
  return (x == null ? true : !(x == null)) ? x : THROW_CCE();
}
