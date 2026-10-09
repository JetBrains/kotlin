function foo(x) {
  return x;
}
function box() {
  var result = foo(2) + 1 | 0;
  if (!(result === 3))
    return 'fail: ' + result;
  return 'OK';
}
