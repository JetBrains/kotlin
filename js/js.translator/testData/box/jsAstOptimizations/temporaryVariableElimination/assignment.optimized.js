function test(a, b, c) {
  var tmp;
  tmp = a + b | 0;
  return tmp + c | 0;
}
function box() {
  var result = test(2, 3, 4);
  if (!(result === 9))
    return 'fail: ' + result;
  return 'OK';
}
