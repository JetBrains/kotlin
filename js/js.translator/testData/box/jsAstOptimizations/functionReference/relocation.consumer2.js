function consumer2() {
  call(topLevel$ref());
  var tmp = topLevel$ref();
  if (!equals(tmp, topLevel$ref()))
    return 'fail: topLevel is not equal to itself';
  var tmp_0 = new Foo();
  call_0(tmp_0, Foo$bar$ref());
  var tmp_1 = Foo$bar$ref();
  if (!equals(tmp_1, Foo$bar$ref()))
    return 'fail: Foo.bar is not equal to itself';
  return 'OK';
}
