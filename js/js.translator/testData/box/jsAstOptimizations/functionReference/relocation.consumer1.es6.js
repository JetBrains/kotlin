function consumer1() {
  call(topLevel$ref());
  let tmp = topLevel$ref();
  if (!equals(tmp, topLevel$ref()))
    return 'fail: topLevel is not equal to itself';
  let tmp_0 = new Foo();
  call_0(tmp_0, Foo$bar$ref());
  let tmp_1 = Foo$bar$ref();
  if (!equals(tmp_1, Foo$bar$ref()))
    return 'fail: Foo.bar is not equal to itself';
  return 'OK';
}
