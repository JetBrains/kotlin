function test(a, b, c) {
  tmp = a + b | 0;
  return tmp + c | 0;
}
function box() {
  var result = test(2, 3, 4);
  if (!(result === 9))
    return 'fail1: ' + result;
  if (!(tmp === 5))
    return 'fail2: ' + tmp;
  return 'OK';
}
