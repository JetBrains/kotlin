function test() {
  return (!a || (b && c)) && foo() || bar();
}