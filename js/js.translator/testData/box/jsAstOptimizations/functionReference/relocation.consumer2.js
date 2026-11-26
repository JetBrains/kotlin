function consumer2() {
  call(topLevel$ref());
  if (!equals(topLevel$ref(), topLevel$ref()))
    return 'fail: topLevel is not equal to itself';
  call_0(new Foo(), Foo$bar$ref());
  if (!equals(Foo$bar$ref(), Foo$bar$ref()))
    return 'fail: Foo.bar is not equal to itself';
  return 'OK';
}
